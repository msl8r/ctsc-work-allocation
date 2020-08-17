package uk.gov.hmcts.reform.workallocation.service;

import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.hmcts.reform.workallocation.email.IEmailSendingService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.services.DummyEmailSendingService;

import java.util.Date;

@RunWith(SerenityRunner.class)
public class DummyEmailSendingServiceTest {

    @Test
    public void testSend() throws Exception {
        IEmailSendingService service = new DummyEmailSendingService();
        long start = new Date().getTime();
        service.sendEmail(Task.builder().build(), "");
        long end = new Date().getTime();
        Assert.assertTrue(end - start >= 2000);
    }
}
