package uk.gov.hmcts.reform.workallocation.email;

import uk.gov.hmcts.reform.workallocation.model.Task;

public interface IEmailSendingService {

    void sendEmail(Task task, String deeplinkBaseUrl) throws Exception;
}
