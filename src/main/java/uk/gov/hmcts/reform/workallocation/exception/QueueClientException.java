package uk.gov.hmcts.reform.workallocation.exception;

public class QueueClientException extends RuntimeException {
    public QueueClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
