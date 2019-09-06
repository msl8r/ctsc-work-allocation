package uk.gov.hmcts.reform.workallocation.services;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;
import uk.gov.hmcts.reform.workallocation.util.MapAppender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;


@Service
@Transactional
public class CcdPollingService {

    private static final Logger log = LoggerFactory.getLogger(CcdPollingService.class);

    public static final String TIME_PLACE_HOLDER = "[TIME]";

    public static final long POLL_INTERVAL = 1000 * 60 * 30L; // 30 minutes

    @Autowired
    private final IdamService idamService;

    @Autowired
    private final CcdClient ccdClient;

    @Autowired
    private final LastRunTimeService lastRunTimeService;

    @Autowired
    private final EmailSendingService emailSendingService;

    @Autowired
    private final QueueProducer<Task> queueProducer;

    @Autowired
    private final QueueConsumer<Task> queueConsumer;

    @Value("${ccd.ctids}")
    private String ctids;

    @Value("${ccd.deeplinkBaseUrl}")
    private String deeplinkBaseUrl;

    private String queryTemplate = "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
        + TIME_PLACE_HOLDER + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
        + "\"operator\": \"or\"}}}]}},\"size\": 500}";

    public CcdPollingService(IdamService idamService, CcdClient ccdClient, LastRunTimeService lastRunTimeService,
                             EmailSendingService emailSendingService, QueueProducer<Task> queueProducer,
                             QueueConsumer<Task> queueConsumer) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.lastRunTimeService = lastRunTimeService;
        this.queueProducer = queueProducer;
        this.emailSendingService = emailSendingService;
        this.queueConsumer = queueConsumer;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public String pollCcdEndpoint() {
        MapAppender.resetLogger();
        StringBuilder resp = new StringBuilder("<html>");

        info("Polling started", resp);

        // 0. get last run time
        LocalDateTime lastRunTime = readLastRunTime();
        info(String.format("Last run time: %s", lastRunTime), resp);

        // 1. Create service token
        String serviceToken = this.idamService.generateServiceAuthorization();

        // 2. create/get user token
        String userAuthToken = this.idamService.getIdamOauth2Token();

        // 3. connect to CCD, and get the data
        // TODO To make some overlap between the runs
        String queryDateTime = lastRunTime.toString();
        Map<String, Object> response = ccdClient.searchCases(userAuthToken, serviceToken, ctids,
            queryTemplate.replace(TIME_PLACE_HOLDER, queryDateTime));
        info("Connecting to CCD was successful", resp);
        info(String.format("Total number of cases: %s", response.get("total")), resp);

        // 4. Process data
        @SuppressWarnings("unchecked")
        List<Map> cases = (List<Map>) response.get("cases");
        List<Either> results = cases.stream().map(o -> {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(o.get("last_modified").toString());
            return Task.builder()
                .id(((Long)o.get("id")).toString())
                .state((String) o.get("state"))
                .jurisdiction((String) o.get("jurisdiction"))
                .caseTypeId((String) o.get("case_type_id"))
                .lastModifiedDate(lastModifiedDate)
                .build();
        }).map(task -> {
            try {
                info(String.format("Task: %s", task), resp);
                emailSendingService.sendEmail(task, deeplinkBaseUrl);
                info("Email sending was successful", resp);
                return Either.right(task);
            } catch (Exception e) {
                info(String.format("Email sending was failed %s", e.getMessage()), resp);
                return Either.left(task);
            }
        }).collect(Collectors.toList());
        info(String.format("Total number of tasks successfully sent: %s",
            results.stream().filter(Either::isRight).count()), resp);
        info(String.format("Total number of tasks failed to send: %s",
            results.stream().filter(Either::isLeft).count()), resp);

        // 5. send to azure service bus
        // disable for now and use directly the mailservice
        // queueProducer.placeItemsInQueue(tasks, Task::getId);

        // 6. write last poll time to file
        lastRunTimeService.updateLastRuntime(LocalDateTime.now());
        return resp.append("</html>").toString();
    }

    private LocalDateTime readLastRunTime() {
        Optional<LocalDateTime> lastRunTime = lastRunTimeService.getLastRunTime();
        return lastRunTime.orElseGet(() -> {
            LocalDateTime defaultLastRun = LastRunTimeService.getMinDate();
            lastRunTimeService.insertLastRunTime(defaultLastRun);
            return defaultLastRun;
        });
    }

    private void info(String message, StringBuilder builder) {
        log.info(message);
        builder.append(message).append("<br/>");
    }

}
