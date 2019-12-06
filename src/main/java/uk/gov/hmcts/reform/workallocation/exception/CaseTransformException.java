package uk.gov.hmcts.reform.workallocation.exception;

public class CaseTransformException extends Exception {

    public CaseTransformException(String message) {
        super(message);
    }

    public CaseTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}
