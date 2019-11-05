package uk.gov.hmcts.reform.workallocation.services;

import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;
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

    public static final String TIME_PLACE_HOLDER = "[TIME]";

    public static final long POLL_INTERVAL = 1000 * 60 * 30L; // 30 minutes

    public static final int LAST_MODIFIED_TIME_MINUS_MINUTES = 5;

    private final TelemetryClient telemetryClient;
    private final IdamService idamService;
    private final CcdClient ccdClient;
    private final LastRunTimeService lastRunTimeService;
    private final QueueProducer<Task> queueProducer;
    private final QueueConsumer<Task> queueConsumer;
    private final DeadQueueConsumer deadQueueConsumer;

    @Value("${ccd.ctids}")
    private String ctids;

    @Value("${ccd.deeplinkBaseUrl}")
    private String deeplinkBaseUrl;

    private String queryTemplate = "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
        + TIME_PLACE_HOLDER + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
        + "\"operator\": \"or\"}}}]}},\"size\": 500}";

    @Autowired
    public CcdPollingService(IdamService idamService, CcdClient ccdClient, LastRunTimeService lastRunTimeService,
                             QueueProducer<Task> queueProducer, QueueConsumer<Task> queueConsumer,
                             DeadQueueConsumer deadQueueConsumer, TelemetryClient telemetryClient) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.lastRunTimeService = lastRunTimeService;
        this.queueProducer = queueProducer;
        this.queueConsumer = queueConsumer;
        this.deadQueueConsumer = deadQueueConsumer;
        this.telemetryClient = telemetryClient;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void pollCcdEndpoint() {
        LocalDateTime lastRunTime = null;
        try {
            telemetryClient.trackEvent("work-allocation start polling");
            final DelayedExecutor delayedExecutor = new DelayedExecutor(Executors.newScheduledThreadPool(1));

            // 0. get last run time
            lastRunTime = readLastRunTime();
            log.info("last run time: {}", lastRunTime);
            LocalDateTime now = LocalDateTime.now();
            long minutes = lastRunTime.until(now, ChronoUnit.MINUTES);
            if (minutes < 29) {
                log.info("The last run was {} minutes ago", minutes);
                return;
            }
            // write last poll time to db, we will roll-back if there is an error
            lastRunTimeService.updateLastRuntime(now);

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

            // 1. Create service token
            String serviceToken = this.idamService.generateServiceAuthorization();

            // 2. create/get user token
            String userAuthToken = this.idamService.getIdamOauth2Token();

            // 3. connect to CCD, and get the data
            // TODO  Properly setup overlap between the runs
            // TODO Change the query to include end time as well
            String queryDateTime = lastRunTime.minusSeconds(LAST_MODIFIED_TIME_MINUS_MINUTES).toString();
            Map<String, Object> response = searchCases(userAuthToken, serviceToken, queryDateTime);
            log.info("Connecting to CCD was successful");
            log.info("total number of cases: {}", response.get("total"));
            telemetryClient.trackMetric("num_of_cases", (Integer) response.get("total"));

            // 4. Process data
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

            // 5. send to azure service bus
            queueProducer.placeItemsInQueue(tasks, Task::getId);
        } catch (Exception e) {
            log.error("Failed to run poller", e);
            if (lastRunTime != null) {
                lastRunTimeService.updateLastRuntime(lastRunTime);
            }
        }
    }

    private Map<String, Object> searchCases(String userAuthToken, String serviceToken, String queryDateTime)
            throws CcdConnectionException {
        Map<String, Object> response;
        try {
            response = ccdClient.searchCases(userAuthToken, serviceToken, ctids,
                queryTemplate.replace(TIME_PLACE_HOLDER, queryDateTime));
        } catch (Exception e) {
            throw new CcdConnectionException("Failed to connect ccd.", e);
        }
        return response;
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
