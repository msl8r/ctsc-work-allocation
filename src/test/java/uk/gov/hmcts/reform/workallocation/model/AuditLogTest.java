package uk.gov.hmcts.reform.workallocation.model;

import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

@RunWith(SerenityRunner.class)
public class AuditLogTest {

    @Test
    public void testGetTime() {
        AuditLog log = new AuditLog();
        log.setTime(1568129762000L);
        LocalDateTime logTime = log.getTime();
        ZonedDateTime logTimeZoned = logTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
        assertEquals(
            LocalDateTime.of(2019, 9, 10, 15, 36, 02),
            logTimeZoned.toLocalDateTime());
    }

    @Test
    public void testToString() {
        AuditLog log = new AuditLog();
        log.setTime(1568129762000L);
        log.setId(1);
        log.setLevel("INFO");
        log.setMessage("test message");

        LocalDateTime logTime = log.getTime();
        ZonedDateTime logTimeZoned = logTime.atZone(ZoneId.systemDefault());

        assertEquals(logTimeZoned.toLocalDateTime().toString() + " - INFO - test message", log.toString());
    }
}
