package uk.gov.hmcts.reform.workallocation.services;

import javax.mail.Message;
import javax.mail.MessagingException;

public interface MailSender {

    void send(Message message) throws MessagingException;
}
