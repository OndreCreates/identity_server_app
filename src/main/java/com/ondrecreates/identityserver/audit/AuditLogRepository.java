package com.ondrecreates.identityserver.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Backs the login/MFA lockout check -- counts recent failures for one user within a sliding window. */
    long countByUserIdAndEventTypeAndCreatedAtAfter(Long userId, AuditEventType eventType, Instant since);

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
