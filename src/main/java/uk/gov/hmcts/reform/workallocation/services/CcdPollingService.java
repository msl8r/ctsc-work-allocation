package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class CcdPollingService {

    public static final String TIME_PLACE_HOLDER = "[TIME]";

    public static final long POLL_INTERVAL = 1000 * 60 * 30; // 30 minutes

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
            queryTemplate.replace("[TIME]", queryDateTime));
        log.info("Connecting to CCD was successful");
        log.info("total number of cases: " + response.get("total").toString());

        // 4. Process data

        // 5. send to azure service bus

        // 6. write last poll time to file
        writeLastRuntime();
    }

    private LocalDateTime readLastRunTime() throws IOException {
        File logFile = new File(logFileName);
        logFile.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(logFile));
        String formattedDate = br.readLine();
        return StringUtils.isEmpty(formattedDate)
            ? LocalDateTime.of(1980, 1, 1, 11, 0)
            : LocalDateTime.parse(formattedDate);
    }

    private void writeLastRuntime() throws IOException {
        try (FileWriter fw = new FileWriter(logFileName, false)) {
            fw.write(LocalDateTime.now().toString());
        }
    }
}
