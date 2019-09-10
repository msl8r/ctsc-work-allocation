package uk.gov.hmcts.reform.workallocation.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.workallocation.model.AuditLog;
import uk.gov.hmcts.reform.workallocation.repository.AuditLogRepository;
import uk.gov.hmcts.reform.workallocation.services.LoggingService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

public class LoggingServiceTest {

    private LoggingService loggingService;
    private AuditLogRepository repository;
    private List<AuditLog> logs;

    @Before
    public void setUp() {
        repository = Mockito.mock(AuditLogRepository.class);
        loggingService = new LoggingService(repository);
        logs = Arrays.asList(
            AuditLog.builder().id(1).level("INFO").message("message1").time(new Date().getTime()).build(),
            AuditLog.builder().id(2).level("INFO").message("message2").time(new Date().getTime() - 100).build()
        );
    }

    @Test
    public void testGetLogs() {
        Mockito.when(repository.findAll(Sort.by(Sort.Direction.ASC, "time")))
            .thenReturn(logs);
        Assert.assertEquals(logs.get(0), loggingService.getLogs().get(0));
    }

    @Test
    public void testDeleteLogs() {
        loggingService.deleteLogsOlderThanOneDay();
        Mockito.verify(repository, Mockito.times(1)).deleteOlderThen(any(Long.class));
    }
}
