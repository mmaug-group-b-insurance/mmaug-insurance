package com.mmaug.claimsagent.audit;

import com.mmaug.claimsagent.audit.dto.AuditEventResponse;
import com.mmaug.claimsagent.common.util.HashUtil;
import com.mmaug.claimsagent.security.CurrentUser;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent record(AuditAction action, AuditOutcome outcome, String entityType,
                             UUID entityId, UUID claimId, CurrentUser actor,
                             String details) {
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(Instant.now());

        if (actor != null) {
            try {
                event.setActorUserId(UUID.fromString(actor.userId()));
            } catch (IllegalArgumentException ignored) {
                event.setActorUserId(null);
            }
            event.setActorRole(actor.roles().isEmpty() ? null : actor.roles().iterator().next().name());
            event.setActorType("USER");
        } else {
            event.setActorType("SYSTEM");
        }

        event.setAction(action);
        event.setOutcome(outcome);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setClaimId(claimId);
        event.setCorrelationId(MDC.get("correlationId"));
        event.setDetails(details);

        // Hash chain: SHA-256(prev_hash || payload)
        String prevHash = repository.findLatest().map(AuditEvent::getHash).orElse("GENESIS");
        event.setPrevHash(prevHash);

        String payload = event.getOccurredAt().toString()
                + "|" + event.getAction()
                + "|" + event.getOutcome()
                + "|" + (entityType != null ? entityType : "")
                + "|" + (entityId != null ? entityId : "")
                + "|" + (actor != null ? actor.userId() : "SYSTEM")
                + "|" + (details != null ? details : "");
        event.setHash(HashUtil.sha256(prevHash + ":" + payload));

        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(UUID claimId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEvent> result = (claimId != null)
                ? repository.findAllByClaimIdOrderByOccurredAtDesc(claimId, pageable)
                : repository.findAllByOrderByOccurredAtDesc(pageable);
        return result.map(this::toResponse);
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return AuditEventResponse.builder()
                .id(e.getId())
                .occurredAt(e.getOccurredAt())
                .actor(e.getActorUserId())
                .actorRole(e.getActorRole())
                .actorType(e.getActorType())
                .action(e.getAction().name())
                .outcome(e.getOutcome().name())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .claimId(e.getClaimId())
                .ipAddress(e.getIpAddress())
                .correlationId(e.getCorrelationId())
                .details(e.getDetails())
                .hash(e.getHash())
                .prevHash(e.getPrevHash())
                .build();
    }
}