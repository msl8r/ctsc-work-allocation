package uk.gov.hmcts.reform.workallocation.email;

import org.apache.velocity.VelocityContext;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public interface TypedVelocityContext<T> {

    VelocityContext getVelocityContext(T objectToSend, String deeplinkBaseUrl);

    String getTemplateFileName(T objectToSend);

    void setMimeMessageSubject(T objectToSend, MimeMessage msg) throws MessagingException;
}
