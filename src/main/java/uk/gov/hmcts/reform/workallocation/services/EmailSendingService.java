package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.email.IEmailSendingService;
import uk.gov.hmcts.reform.workallocation.model.Task;

import java.io.StringWriter;
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
public class EmailSendingService implements IEmailSendingService, InitializingBean {

    @Value("${smtp.host}")
    private String smtpHost;

    @Value("${smtp.port}")
    private String smtpPort;

    @Value("${smtp.user}")
    private String smtpUser;

    @Value("${smtp.password}")
    private String smtpPassword;

    @Value("${smtp.from}")
    private String smtpFrom;

    @Value("${service.email}")
    private String serviceEmail;

    @Autowired
    private VelocityEngine velocityEngine;

    private Session session;

    private static final String TEMPLATE_DIR = "templates/";

    private static final String NO_JURISDICTION = "No Jurisdiction";

    @Override
    public void sendEmail(Task task, String deeplinkBaseUrl) throws Exception {
        log.info("Sending Email for Task {} With Deep Link URL {} to Email address {}",
            task, deeplinkBaseUrl, serviceEmail);

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

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(serviceEmail, false));
        Transport.send(msg);
        log.info("Email sending successful");
    }

    private MimeMessage createMimeMessage(String deepLinkUrl) throws MessagingException {
        MimeMessage msg = new MimeMessage(this.session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.addHeader("Case_URL", deepLinkUrl);
        return msg;
    }


    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");

        Authenticator auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
        return Session.getInstance(props, auth);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.session = createSession();
    }
}
