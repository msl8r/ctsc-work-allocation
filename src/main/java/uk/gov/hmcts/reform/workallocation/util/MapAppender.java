package uk.gov.hmcts.reform.workallocation.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapAppender extends AppenderBase<ILoggingEvent> {

    private static ConcurrentMap<String, ILoggingEvent> EVENT_MAP
        = new ConcurrentHashMap<>();

    @Override
    protected void append(ILoggingEvent event) {
        EVENT_MAP.put(LocalDateTime.now().toString(), event);
    }

    public static Map<String, ILoggingEvent> getEventMap() {
        return EVENT_MAP;
    }

    public static void resetLogger() {
        EVENT_MAP.clear();
    }
}
