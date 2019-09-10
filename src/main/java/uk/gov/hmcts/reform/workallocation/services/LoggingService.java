package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.model.AuditLog;
import uk.gov.hmcts.reform.workallocation.repository.AuditLogRepository;

import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;

@Service
@Slf4j
@Transactional
public class LoggingService {

    private static final long POLL_INTERVAL = 4 * 60 * 60 * 1000L;

    private final AuditLogRepository auditLogRepository;

    public LoggingService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> getLogs() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.ASC, "time"));
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void deleteLogsOlderThanOneDay() {
        log.info("Cleaning up db logs");
        long fromTimeToDelete = new Date().getTime() - (24 * 60 * 60 * 1000);
        auditLogRepository.deleteOlderThen(fromTimeToDelete);
    }
}
