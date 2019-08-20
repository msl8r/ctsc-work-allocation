package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.model.Task;

import java.io.StringWriter;
import javax.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailSendingService {

    @Value("${service.email}")
    private String serviceEmail;

    @Value("${smtp.from}")
    private String fromEmail;

    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private JavaMailSender javaMailSender;

    private static final String ENCODING = "UTF-8";

    public void sendEmail(Task task, String deeplinkBaseURL) throws Exception {
        log.info("Sending Email");

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("jurisdiction", task.getJurisdiction());
        velocityContext.put("lastModifiedDate", task.getLastModifiedDate());
        velocityContext.put("deepLinkURL", deeplinkBaseURL + task.getJurisdiction()
            +  "\\" + task.getCaseTypeId() + "\\" + task.getId());

        String templateFileName = task.getJurisdiction() != null
            ? task.getJurisdiction().toLowerCase() + ".vm" :  "divorce.vm";

        StringWriter stringWriter = new StringWriter();
        boolean templateMerged = velocityEngine.mergeTemplate(templateFileName, ENCODING, velocityContext, stringWriter);

        if (templateMerged) {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setTo(serviceEmail);
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setSubject("Service: " + task.getJurisdiction() + ",State:" + task.getState());
            mimeMessageHelper.setText(stringWriter.toString(), true);

            javaMailSender.send(mimeMessage);
            log.info("Email sending successful");
        }
    }
}
