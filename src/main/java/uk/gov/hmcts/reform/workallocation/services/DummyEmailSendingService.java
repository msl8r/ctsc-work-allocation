package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.email.IEmailSendingService;
import uk.gov.hmcts.reform.workallocation.model.Task;

@Slf4j
@Service
@ConditionalOnProperty(name = "smtp.enabled", havingValue = "false")
public class DummyEmailSendingService implements IEmailSendingService {

    @Override
    public void sendEmail(Task task, String deeplinkBaseUrl) throws Exception {
        Thread.sleep(2000);
        log.info("DummyEmailSendingService sent task: {}", task);
    }
}
