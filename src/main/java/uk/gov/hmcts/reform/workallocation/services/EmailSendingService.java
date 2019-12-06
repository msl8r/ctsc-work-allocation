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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Service
@Slf4j
@ConditionalOnProperty(name = "smtp.enabled", havingValue = "true")
public class EmailSendingService implements IEmailSendingService {

    private static final String TEMPLATE_DIR = "templates/";
    private static final String NO_JURISDICTION = "No Jurisdiction";

    private final VelocityEngine velocityEngine;
    private final Session session;
    private final String smtpFrom;
    private final Map<String, String> serviceEmails = new HashMap<>();

    @Autowired
    public EmailSendingService(@Value("${smtp.host}") String smtpHost,
                               @Value("${smtp.port}") String smtpPort,
                               @Value("${smtp.user}") String smtpUser,
                               @Value("${smtp.password}") String smtpPassword,
                               @Value("${service.email}") String divorceServiceEmail,
                               @Value("${service.probate.email}") String probateServiceEmail,
                               VelocityEngine velocityEngine) {
        this.session = createSession(smtpHost, smtpPort, smtpUser, smtpPassword);
        this.velocityEngine = velocityEngine;
        this.smtpFrom = smtpUser;
        this.serviceEmails.put("DIVORCE", divorceServiceEmail);
        this.serviceEmails.put("PROBATE", probateServiceEmail);
    }

    @Override
    public void sendEmail(Task task, String deeplinkBaseUrl) throws EmailSendingException {
        String emailTo = serviceEmails.get(task.getJurisdiction());
        log.info("Sending Email for Task {} With Deep Link URL {} to Email address {}",
            task, deeplinkBaseUrl, emailTo);

        try {
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("jurisdiction", task.getJurisdiction());
            velocityContext.put("lastModifiedDate", task.getLastModifiedDate());
            velocityContext.put("deepLinkUrl", deeplinkBaseUrl + task.getJurisdiction()
                +  "/" + task.getCaseTypeId() + "/" + task.getId());
            String jurisdiction = task.getJurisdiction() != null ? task.getJurisdiction() : NO_JURISDICTION;
            String templateFileName = jurisdiction.equalsIgnoreCase(NO_JURISDICTION)
                ? "default.vm"
                : jurisdiction.toLowerCase() + ".vm";

            StringWriter stringWriter = new StringWriter();
            Template template = velocityEngine.getTemplate(TEMPLATE_DIR + templateFileName);
            template.merge(velocityContext, stringWriter);

            MimeMessage msg = createMimeMessage(deeplinkBaseUrl);
            msg.setReplyTo(InternetAddress.parse(smtpFrom, false));
            msg.setFrom(InternetAddress.parse(smtpFrom, false)[0]);
            msg.setSubject(task.getId() + " - " + task.getState() + " - " + jurisdiction.toUpperCase(), "UTF-8");
            msg.setText(stringWriter.toString(), "UTF-8", "html");

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false));
            Transport.send(msg);
            log.info("Email sending successful");
        } catch (Exception e) {
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    private MimeMessage createMimeMessage(String deepLinkUrl) throws MessagingException {
        MimeMessage msg = new MimeMessage(this.session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.addHeader("Case_URL", deepLinkUrl);
        return msg;
    }


    private Session createSession(String smtpHost, String smtpPort, String smtpUser, String smtpPassword) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
        return Session.getInstance(props, auth);
    }
}
