package com.mmaug.claimsagent.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Insert-only repository. There are no update or delete methods.
 * The database role should also have UPDATE and DELETE revoked on the audit_event table.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("SELECT a FROM AuditEvent a WHERE a.id = (SELECT MAX(b.id) FROM AuditEvent b)")
    Optional<AuditEvent> findLatest();

    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    Page<AuditEvent> findAllByClaimIdOrderByOccurredAtDesc(UUID claimId, Pageable pageable);
}