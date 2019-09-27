package uk.gov.hmcts.reform.workallocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.workallocation.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.time < :timestamp")
    int deleteOlderThen(@Param("timestamp") Long timestamp);
}
