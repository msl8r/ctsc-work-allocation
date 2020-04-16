package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.email.IEmailSendingService;
import uk.gov.hmcts.reform.workallocation.exception.EmailSendingException;
import uk.gov.hmcts.reform.workallocation.model.Task;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
@ConditionalOnProperty(name = "smtp.enabled", havingValue = "true")
public class EmailSendingService implements IEmailSendingService {

    private static final String TEMPLATE_DIR = "templates/";
    private static final String NO_JURISDICTION = "No Jurisdiction";

    private final VelocityEngine velocityEngine;
    private final Map<String, Session> serviceEmailSessions = new HashMap<>();

    @Autowired
    public EmailSendingService(@Value("${imap.host}") String imapHost,
                               @Value("${imap.port}") String imapPort,
                               @Value("${service.divorce.email}") String divorceServiceEmail,
                               @Value("${service.divorce.password}") String divorceServicePassword,
                               @Value("${service.probate.email}") String probateServiceEmail,
                               @Value("${service.probate.password}") String probateServicePassword,
                               VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
        this.serviceEmailSessions.put("DIVORCE",
            createSession(imapHost, imapPort, divorceServiceEmail, divorceServicePassword));
        this.serviceEmailSessions.put("PROBATE",
            createSession(imapHost, imapPort, probateServiceEmail, probateServicePassword));
    }

    @Override
    public void sendEmail(Task task, String deeplinkBaseUrl) throws EmailSendingException {
        Session session = serviceEmailSessions.get(task.getJurisdiction());
        log.info("Creating Email for Task {} With Deep Link URL {} in {} inbox",
            task, deeplinkBaseUrl, session.getProperty(""));

        try {
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("jurisdiction", task.getJurisdiction());
            velocityContext.put("lastModifiedDate", task.getLastModifiedDate());
            // it's an ugly hack now but the url structure of prod/non prod ccd is very different
            if (deeplinkBaseUrl.contains("www.ccd.platform.hmcts.net")) {
                velocityContext.put("deepLinkUrl", deeplinkBaseUrl + task.getId());
            } else {
                velocityContext.put("deepLinkUrl", deeplinkBaseUrl + task.getJurisdiction()
                    +  "/" + task.getCaseTypeId() + "/" + task.getId());
            }
            String jurisdiction = task.getJurisdiction() != null ? task.getJurisdiction() : NO_JURISDICTION;
            String templateFileName = jurisdiction.equalsIgnoreCase(NO_JURISDICTION)
                ? "default.vm"
                : jurisdiction.toLowerCase() + ".vm";

            StringWriter stringWriter = new StringWriter();
            Template template = velocityEngine.getTemplate(TEMPLATE_DIR + templateFileName);
            template.merge(velocityContext, stringWriter);

            MimeMessage msg = createMimeMessage(deeplinkBaseUrl, session);
            msg.setSubject(task.getId() + " - " + task.getState() + " - " + jurisdiction.toUpperCase(), "UTF-8");
            msg.setText(stringWriter.toString(), "UTF-8", "html");

            Store store = session.getStore("imap");
            store.connect();
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_WRITE);
            folderInbox.appendMessages(new Message[]{msg});
            log.info("Email sending successful");
        } catch (Exception e) {
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    private MimeMessage createMimeMessage(String deepLinkUrl, Session session) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.addHeader("Case_URL", deepLinkUrl);
        return msg;
    }


    private Session createSession(String imapHost, String imapPort, String smtpUser, String smtpPassword) {
        Properties props = new Properties();
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.imap.socketFactory.port", imapPort);

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
        return Session.getInstance(props, auth);
    }
}
