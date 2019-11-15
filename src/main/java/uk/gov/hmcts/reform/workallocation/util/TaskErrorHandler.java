package uk.gov.hmcts.reform.workallocation.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

import javax.persistence.OptimisticLockException;

@Slf4j
public class TaskErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable t) {
        if (t instanceof OptimisticLockException) {
            log.info("An instance is already running");
        } else {
            log.error("Something went wrong", t);
        }

    }
}
