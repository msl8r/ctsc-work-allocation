package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.model.Task;

import java.io.StringWriter;
import java.util.Date;
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
public class EmailSendingService implements InitializingBean {

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

    public void sendEmail(Task task, String deeplinkBaseUrl) throws Exception {
        log.info("Sending Email");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("jurisdiction", task.getJurisdiction());
        velocityContext.put("lastModifiedDate", task.getLastModifiedDate());
        velocityContext.put("deepLinkUrl", deeplinkBaseUrl + task.getJurisdiction()
            +  "/" + task.getCaseTypeId() + "/" + task.getId());

        String templateFileName = task.getJurisdiction() != null
            ? task.getJurisdiction().toLowerCase() + ".vm" :  "divorce.vm";

        StringWriter stringWriter = new StringWriter();
        final Template template = velocityEngine.getTemplate("templates/" + templateFileName);
        if (template == null) {
            throw new RuntimeException("Template " + templateFileName + " not found ");
        }
        template.merge(velocityContext, stringWriter);

        MimeMessage msg = creatMimeMessage();
        msg.setReplyTo(InternetAddress.parse(smtpFrom, false));
        msg.setSubject("Service: " + task.getJurisdiction() + ",State:" + task.getState(), "UTF-8");
        msg.setText(stringWriter.toString(), "UTF-8");
        msg.setSentDate(new Date());

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(serviceEmail, false));
        Transport.send(msg);
        log.info("Email sending successful");
    }

    private MimeMessage creatMimeMessage() throws MessagingException {
        MimeMessage msg = new MimeMessage(this.session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        return msg;
    }


    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

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
