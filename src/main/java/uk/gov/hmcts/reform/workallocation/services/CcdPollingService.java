package uk.gov.hmcts.reform.workallocation.services;

import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.DeadQueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.DelayedExecutor;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class CcdPollingService {

    private static final Logger log = LoggerFactory.getLogger(CcdPollingService.class);

    private final TelemetryClient telemetryClient;
    private final IdamService idamService;
    private final CcdConnectorService ccdConnectorService;
    private final LastRunTimeService lastRunTimeService;
    private final QueueProducer<Task> queueProducer;
    private final QueueConsumer<Task> queueConsumer;
    private final DeadQueueConsumer deadQueueConsumer;

    private final int lastModifiedTimeMinusMinutes;
    private final int pollIntervalMinutes;

    @Autowired
    public CcdPollingService(IdamService idamService, CcdConnectorService ccdConnectorService,
                             LastRunTimeService lastRunTimeService,
                             @Value("${service.poll_interval_minutes}") int pollIntervalMinutes,
                             @Value("${service.last_modified_minus_minutes}") int lastModifiedTimeMinusMinutes,
                             QueueProducer<Task> queueProducer, QueueConsumer<Task> queueConsumer,
                             DeadQueueConsumer deadQueueConsumer, TelemetryClient telemetryClient) {
        this.idamService = idamService;
        this.ccdConnectorService = ccdConnectorService;
        this.lastRunTimeService = lastRunTimeService;
        this.queueProducer = queueProducer;
        this.queueConsumer = queueConsumer;
        this.deadQueueConsumer = deadQueueConsumer;
        this.telemetryClient = telemetryClient;
        this.lastModifiedTimeMinusMinutes = lastModifiedTimeMinusMinutes;
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    @Scheduled(cron = "${service.poll_cron}")
    public void pollCcdEndpoint() {
        LocalDateTime lastRunTime = null;
        try {
            telemetryClient.trackEvent("work-allocation start polling");

            // 0. get last run time
            lastRunTime = readLastRunTime();
            log.info("last run time: {}", lastRunTime);
            LocalDateTime now = LocalDateTime.now();
            long minutes = lastRunTime.until(now, ChronoUnit.MINUTES);
            if (minutes < pollIntervalMinutes) {
                log.info("The last run was {} minutes ago", minutes);
                return;
            }
            // write last poll time to db, we will roll-back if there is an error
            lastRunTimeService.updateLastRuntime(now);

            // 1. Start polling the queue
            final DelayedExecutor delayedExecutor = new DelayedExecutor(Executors.newScheduledThreadPool(1));
            // Handling dead letters
            log.info("collecting dead letters");
            deadQueueConsumer
                .runConsumer(delayedExecutor)
                .thenCompose(aVoid -> {
                    // Start queue client
                    log.info("poll started");
                    return queueConsumer.runConsumer(delayedExecutor);
                }).whenComplete((aVoid, throwable) -> {
                    if (throwable != null) {
                        log.error("There was an error running queue client", throwable);
                    }
                    delayedExecutor.shutdown();
                });

            // 2. Create service token
            String serviceToken = this.idamService.generateServiceAuthorization();

            // 3. create/get user token
            String userAuthToken = this.idamService.getIdamOauth2Token();

            // 4. connect to CCD, and get the data
            // TODO  Properly setup overlap between the runs
            // TODO Change the query to include end time as well
            String queryDateTime = lastRunTime.minusMinutes(lastModifiedTimeMinusMinutes).toString();
            Map<String, Object> response = ccdConnectorService.searchCases(userAuthToken, serviceToken, queryDateTime);
            log.info("Connecting to CCD was successful");
            log.info("total number of cases: {}", response.get("total"));
            telemetryClient.trackMetric("num_of_cases", (Integer) response.get("total"));

            // 5. Process data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cases = (List<Map<String, Object>>) response.get("cases");
            List<Task> tasks = cases.stream().map(o -> {
                try {
                    return Task.fromCcdDCase(o);
                } catch (Exception e) {
                    log.error("Failed to parse case", e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            log.info("total number of tasks: {}", tasks.size());
            telemetryClient.trackMetric("num_of_tasks", tasks.size());

            // 6. send to azure service bus
            queueProducer.placeItemsInQueue(tasks, Task::getId);
        } catch (Exception e) {
            log.error("Failed to run poller", e);
            if (lastRunTime != null) {
                lastRunTimeService.updateLastRuntime(lastRunTime);
            }
        }
    }

    private LocalDateTime readLastRunTime() {
        Optional<LocalDateTime> lastRunTime = lastRunTimeService.getLastRunTime();
        return lastRunTime.orElseGet(() -> {
            LocalDateTime defaultLastRun = lastRunTimeService.getMinDate();
            lastRunTimeService.insertLastRunTime(defaultLastRun);
            return defaultLastRun;
        });
    }

}
