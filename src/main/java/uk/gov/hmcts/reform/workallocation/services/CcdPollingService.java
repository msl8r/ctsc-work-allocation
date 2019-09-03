package uk.gov.hmcts.reform.workallocation.services;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
                             QueueProducer<Task> queueProducer, QueueConsumer<Task> queueConsumer) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.lastRunTimeService = lastRunTimeService;
        this.queueProducer = queueProducer;
        this.queueConsumer = queueConsumer;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void pollCcdEndpoint() throws ServiceBusException, InterruptedException {
        log.info("poll started");

        // -1. Start queue client
        queueConsumer.registerReceiver();

        // 0. get last run time
        LocalDateTime lastRunTime = readLastRunTime();
        log.info("last run time: {}", lastRunTime);

        // 1. Create service token
        String serviceToken = this.idamService.generateServiceAuthorization();

        // 2. create/get user token
        String userAuthToken = this.idamService.getIdamOauth2Token();

        // 3. connect to CCD, and get the data
        // TODO To make some overlap between the runs
        String queryDateTime = lastRunTime.toString();
        Map<String, Object> response = ccdClient.searchCases(userAuthToken, serviceToken, ctids,
            queryTemplate.replace(TIME_PLACE_HOLDER, queryDateTime));
        log.info("Connecting to CCD was successful");
        log.info("total number of cases: {}", response.get("total"));

        // 4. Process data
        @SuppressWarnings("unchecked")
        List<Map> cases = (List<Map>) response.get("cases");
        List<Task> tasks = cases.stream().map(o -> {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(o.get("last_modified").toString());
            return Task.builder()
                .id(((Long)o.get("id")).toString())
                .state((String) o.get("state"))
                .jurisdiction((String) o.get("jurisdiction"))
                .caseTypeId((String) o.get("case_type_id"))
                .lastModifiedDate(lastModifiedDate)
                .build();
        }).collect(Collectors.toList());
        log.info("total number of tasks: {}", tasks.size());

        // 5. send to azure service bus
        queueProducer.placeItemsInQueue(tasks, Task::getId);

        // 6. write last poll time to file
        lastRunTimeService.updateLastRuntime(LocalDateTime.now());
    }

    private LocalDateTime readLastRunTime() {
        Optional<LocalDateTime> lastRunTime = lastRunTimeService.getLastRunTime();
        return lastRunTime.orElseGet(() -> {
            LocalDateTime defaultLastRun = LastRunTimeService.getMinDate();
            lastRunTimeService.insertLastRunTime(defaultLastRun);
            return defaultLastRun;
        });
    }

}
