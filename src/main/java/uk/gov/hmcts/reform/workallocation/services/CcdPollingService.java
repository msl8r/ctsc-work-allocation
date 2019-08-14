package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

@Service
@Slf4j
@Transactional
public class CcdPollingService {

    public static final String TIME_PLACE_HOLDER = "[TIME]";

    public static final long POLL_INTERVAL = 1000 * 60 * 30L; // 30 minutes

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private final IdamService idamService;

    @Autowired
    private final CcdClient ccdClient;

    @Value("${ccd.ctids}")
    private String ctids;

    @Value("${last-run-log}")
    private String logFileName;

    private String queryTemplate = "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
        + TIME_PLACE_HOLDER + "\"}}},{\"match\":{\"state\":\"Submitted\"}}]}}}";

    public CcdPollingService(IdamService idamService, CcdClient ccdClient) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void pollCcdEndpoint() throws IOException {
        log.info("poll started");

        // 0. get last run time
        LocalDateTime lastRunTime = readLastRunTime();
        log.info("last run time: " + lastRunTime);

        // 1. Create service token
        String serviceToken = this.idamService.generateServiceAuthorization();

        // 2. create/get user token
        String userAuthToken = this.idamService.getIdamOauth2Token();

        // 3. connect to CCD, and get the data
        String queryDateTime = lastRunTime.minusMinutes(30).toString();
        Map<String, Object> response = ccdClient.searchCases(userAuthToken, serviceToken, ctids,
            queryTemplate.replace(TIME_PLACE_HOLDER, queryDateTime));
        log.info("Connecting to CCD was successful");
        log.info("total number of cases: " + response.get("total").toString());

        // 4. Process data

        // 5. send to azure service bus

        // 6. write last poll time to file
        updateLastRuntime();
    }

    private LocalDateTime readLastRunTime() throws IOException {
        Query q = em.createNativeQuery("select last_run from last_run_time where id = :id").setParameter("id", 1);
        LocalDateTime lastRunTime = null;
        try {
            Date lastRun = (Date) q.getSingleResult();
            lastRunTime = LocalDateTime.ofInstant(lastRun.toInstant(), ZoneId.systemDefault());
        } catch (NoResultException e) {
            q = em.createNativeQuery("insert into last_run_time (id, last_run) values (:id, :lastRun)")
                .setParameter("id", 1)
                .setParameter("lastRun", LocalDateTime.now());
            q.executeUpdate();
            log.info("Can't find last run in db");
        }
        return lastRunTime == null ? LocalDateTime.of(1980, 1, 1, 11, 0) : lastRunTime;
    }

    private void updateLastRuntime() {
        Query q = em.createNativeQuery("update last_run_time set last_run = :lastRun where id = :id")
            .setParameter("lastRun", LocalDateTime.now())
            .setParameter("id", 1);
        q.executeUpdate();
    }
}
