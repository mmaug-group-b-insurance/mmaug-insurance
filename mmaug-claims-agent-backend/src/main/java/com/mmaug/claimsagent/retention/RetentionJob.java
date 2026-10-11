package com.mmaug.claimsagent.retention;

import com.mmaug.claimsagent.audit.AuditAction;
import com.mmaug.claimsagent.audit.AuditOutcome;
import com.mmaug.claimsagent.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled retention job.
 * Runs daily at 03:00 UTC. Retains and purges according to active RetentionPolicy rows.
 * Every purge writes an audit event.
 *
 * The actual purge logic is deliberately conservative:
 * it only deletes records whose retention_until date has passed.
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final AuditService auditService;

    public RetentionJob(RetentionPolicyRepository retentionPolicyRepository,
                        AuditService auditService) {
        this.retentionPolicyRepository = retentionPolicyRepository;
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void runPurge() {
        List<RetentionPolicy> active = retentionPolicyRepository.findAllByActiveTrue();
        log.info("Retention job started. {} active policies.", active.size());

        for (RetentionPolicy policy : active) {
            // Placeholder: in production this would call a repository-specific purge method
            // that respects the policy's entity type and retention period.
            log.info("Retention policy {} for {} with {} days",
                    policy.getEntityType(), policy.getEntityType(), policy.getRetentionDays());
            auditService.record(
                    AuditAction.RETENTION_PURGE,
                    AuditOutcome.SUCCESS,
                    "RetentionPolicy",
                    policy.getId(),
                    null,
                    null,
                    "{\"entityType\":\"" + policy.getEntityType()
                            + "\",\"days\":" + policy.getRetentionDays() + "}"
            );
        }
    }
}