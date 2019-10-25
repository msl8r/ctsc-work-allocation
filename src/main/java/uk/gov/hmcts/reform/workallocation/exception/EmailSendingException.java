package uk.gov.hmcts.reform.workallocation.exception;

public class EmailSendingException extends Exception {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
