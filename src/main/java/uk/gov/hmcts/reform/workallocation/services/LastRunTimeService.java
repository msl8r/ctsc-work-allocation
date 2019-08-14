package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;


@Service
@Transactional
@Slf4j
public class LastRunTimeService {

    @PersistenceContext
    private EntityManager em;

    public static LocalDateTime getMinDate() {
        return LocalDateTime.of(1980, 1, 1, 11, 0);
    }

    public Optional<LocalDateTime> getLastRunTime() {
        Query q = em.createNativeQuery("select last_run from last_run_time where id = :id").setParameter("id", 1);
        LocalDateTime lastRunTime = null;
        try {
            Date lastRun = (Date) q.getSingleResult();
            lastRunTime = LocalDateTime.ofInstant(lastRun.toInstant(), ZoneId.systemDefault());
        } catch (NoResultException e) {
            log.info("Can't find last run in db");
        }
        return Optional.ofNullable(lastRunTime);
    }

    public void insertLastRunTime(LocalDateTime time) {
        Query q = em.createNativeQuery("insert into last_run_time (id, last_run) values (:id, :lastRun)")
            .setParameter("id", 1)
            .setParameter("lastRun", time);
        q.executeUpdate();
    }

    public void updateLastRuntime(LocalDateTime time) {
        Query q = em.createNativeQuery("update last_run_time set last_run = :lastRun where id = :id")
            .setParameter("lastRun", time)
            .setParameter("id", 1);
        q.executeUpdate();
    }

}
