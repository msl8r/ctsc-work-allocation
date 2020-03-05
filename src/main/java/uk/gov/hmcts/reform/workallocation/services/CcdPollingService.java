package uk.gov.hmcts.reform.workallocation.services;

import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;
import uk.gov.hmcts.reform.workallocation.idam.IdamConnectionException;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.DeadQueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.DelayedExecutor;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

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
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = {Exception.class})
    public void pollCcdEndpoint() throws IdamConnectionException, CcdConnectionException {
        telemetryClient.trackEvent("work-allocation start polling");

        // 0. get last run time
        LocalDateTime lastRunTime = readLastRunTime();
        log.info("last run time: {}", lastRunTime);
        LocalDateTime now = LocalDateTime.now();
        long minutes = lastRunTime.until(now, ChronoUnit.MINUTES);
        if (minutes < pollIntervalMinutes) {
            log.info("The last run was {} minutes ago", minutes);
            return;
        }
        // write last poll time to db, we will roll-back if there is an
        lastRunTimeService.updateLastRuntime(now);

        // 1. Start polling the queue
        final DelayedExecutor delayedExecutor = new DelayedExecutor(Executors.newScheduledThreadPool(1));
        // Handling dead letters
        log.info("collecting dead letters");
        deadQueueConsumer
            .runConsumer(delayedExecutor, now.plusMinutes(20))
            .thenCompose(aVoid -> {
                // Start queue client
                log.info("poll started");
                return queueConsumer.runConsumer(delayedExecutor, now.plusMinutes(20));
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
        String queryFromDateTime = lastRunTime.minusMinutes(lastModifiedTimeMinusMinutes).toString();
        String queryToDateTime = now.minusMinutes(lastModifiedTimeMinusMinutes).toString();
        // Divorce cases
        Map<String, Object> divorceData = ccdConnectorService.searchDivorceCases(userAuthToken, serviceToken,
            queryFromDateTime, queryToDateTime);
        log.info("Connecting to CCD was successful");
        log.info("total number of divorce cases: {}", divorceData.get("total"));
        telemetryClient.trackMetric("num_of_divorce_cases", (Integer) divorceData.get("total"));

        // Probate cases
        Map<String, Object> probateData = ccdConnectorService.searchProbateCases(userAuthToken, serviceToken,
            queryFromDateTime, queryToDateTime);
        log.info("Connecting to CCD was successful");
        log.info("total number of probate cases: {}", probateData.get("total"));
        telemetryClient.trackMetric("num_of_probate_cases", (Integer) probateData.get("total"));

        // 5. Process data
        @SuppressWarnings("unchecked")
        List<Task> tasks = mergeResponse(divorceData, probateData);
        log.info("total number of tasks: {}", tasks.size());
        telemetryClient.trackMetric("num_of_tasks", tasks.size());

        // 6. send to azure service bus
        queueProducer.placeItemsInQueue(tasks, Task::getId);
    }

    private LocalDateTime readLastRunTime() {
        Optional<LocalDateTime> lastRunTime = lastRunTimeService.getLastRunTime();
        return lastRunTime.orElseGet(() -> {
            LocalDateTime defaultLastRun = lastRunTimeService.getMinDate();
            lastRunTimeService.insertLastRunTime(defaultLastRun);
            return defaultLastRun;
        });
    }

    @SuppressWarnings("unchecked")
    private List<Task> mergeResponse(Map<String, Object>... data) {
        List<Task> tasks = new ArrayList<>();
        Arrays.stream(data).forEach(stringObjectMap -> {
            String caseTypeId = (String)stringObjectMap.get("case_type_id");
            List<Map<String, Object>> cases = (List<Map<String, Object>>) stringObjectMap.get("cases");
            cases.stream().forEach(o -> {
                try {
                    tasks.add(Task.fromCcdCase(o, caseTypeId));
                } catch (Exception e) {
                    log.error("Failed to parse case", e);
                }
            });
        });
        return tasks;
    }

}
