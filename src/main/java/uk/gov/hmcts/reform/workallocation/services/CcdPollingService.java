package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;

@Service
@Slf4j
public class CcdPollingService {

    public static final long POLL_INTERVAL = 1800000; // 30 minutes

    @Autowired
    private final IdamService idamService;

    public CcdPollingService(IdamService idamService) {
        this.idamService = idamService;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void pollCCDEndpoint() {
        log.info("poll started");
        // 1. Create service token
        String oneTimePassword = this.idamService.generateServiceAuthorization();
        log.info(oneTimePassword);

        // 2. create/get user token

        // 3. connect to CCD
        // 4. get the data
        // 5. send to azure service bus
    }

    private void createServiceToken() {

    }
}
