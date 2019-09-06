package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.model.AuditLog;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

@Service
@Slf4j
@Transactional
public class LoggingService {

    private static final long POLL_INTERVAL = 4 * 60 * 60 * 1000;

    @PersistenceContext
    private EntityManager em;

    public List<AuditLog> getLogs() {
        String strQuery = "from AuditLog";
        return em.createQuery(strQuery, AuditLog.class).getResultList();
    }

    @Scheduled(fixedDelay = POLL_INTERVAL)
    public void deleteLogsOlderThanOneDay() {
        log.info("Cleaning up db logs");
        Query query = em.createQuery(
            "DELETE FROM AuditLog al WHERE al.time < :t");
        long fromTimeToDelete = new Date().getTime() - (24 * 60 * 60 * 1000);
        query.setParameter("t", fromTimeToDelete).executeUpdate();
    }
}
