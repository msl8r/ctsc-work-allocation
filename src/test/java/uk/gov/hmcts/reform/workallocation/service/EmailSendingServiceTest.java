package uk.gov.hmcts.reform.workallocation.service;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.workallocation.exception.EmailSendingException;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.services.EmailSendingService;
import uk.gov.hmcts.reform.workallocation.services.MailSender;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;

public class EmailSendingServiceTest {

    private EmailSendingService service;
    private Message message;
    private MailSender mailSender = (msg) -> message = msg;

    @Before
    public void setUp() throws Exception {
        service = new EmailSendingService("smtp.google.com", "123", "user", "password",
            "CMC:cmc@mail.com,PROBATE:prob@mail.com,DIVORCE:div@mail.com",
            getVelocityEngine(), mailSender);
    }

    @Test
    public void testSendEmail() throws EmailSendingException, MessagingException {
        File folder = new File("");
        System.out.println(folder.getAbsolutePath());
        Task task = Task.builder()
            .jurisdiction("DIVORCE")
            .lastModifiedDate(LocalDateTime.now())
            .state("Submitted")
            .id("1563460551495313")
            .caseTypeId("DIVORCE")
            .build();
        service.sendEmail(task, "http://mydomain.local/case");
        Assert.assertEquals("div@mail.com", message.getAllRecipients()[0].toString());
        Assert.assertEquals("1563460551495313 - Submitted - DIVORCE", message.getSubject());
    }

    private VelocityEngine getVelocityEngine() throws Exception {
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class",
            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine engine = new VelocityEngine(props);
        engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
            "org.apache.velocity.runtime.log.Log4JLogChute");

        engine.setProperty("runtime.log.logsystem.log4j.logger", "VELOCITY_LOGGER");
        engine.init();
        return engine;
    }
}
