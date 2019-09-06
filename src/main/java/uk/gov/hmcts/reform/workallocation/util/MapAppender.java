package uk.gov.hmcts.reform.workallocation.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MapAppender extends AppenderBase<ILoggingEvent> {

    private static List<String> EVENT_MAP
        = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        EVENT_MAP.add(LocalDateTime.now() + ": " + event);
    }

    public static List<String> getEventMap() {
        return EVENT_MAP;
    }

    public static void resetLogger() {
        EVENT_MAP.clear();
    }
}
