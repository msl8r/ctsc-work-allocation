package uk.gov.hmcts.reform.workallocation.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MemoryAppender extends AppenderBase<ILoggingEvent> {

    private static List<String> eventLog = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        eventLog.add(LocalDateTime.now() + ": " + event);
    }

    public static List<String> getEventLog() {
        return eventLog;
    }

    public static void resetLogger() {
        eventLog.clear();
    }
}
