package uk.gov.hmcts.reform.workallocation.model;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class AuditLogTest {

    @Test
    public void testGetTime() {
        AuditLog log = new AuditLog();
        log.setTime(1568129762000L);
        Assert.assertEquals(LocalDateTime.of(2019, 9, 10, 16, 36, 02), log.getTime());
    }

    @Test
    public void testToString() {
        AuditLog log = new AuditLog();
        log.setTime(1568129762000L);
        log.setId(1);
        log.setLevel("INFO");
        log.setMessage("test message");

        Assert.assertEquals("2019-09-10T16:36:02 - INFO - test message", log.toString());
    }
}
