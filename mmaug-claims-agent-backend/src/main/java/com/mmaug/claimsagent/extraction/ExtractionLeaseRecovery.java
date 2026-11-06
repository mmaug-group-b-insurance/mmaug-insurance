package com.mmaug.claimsagent.extraction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class ExtractionLeaseRecovery {
    private final JdbcTemplate jdbc;

    public ExtractionLeaseRecovery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int recoverExpired() {
        // One statement atomically retires the attempt and its document reservation.
        return jdbc.update("""
                with expired as (
                    select id, extraction_token from claim_document
                    where processing_status='EXTRACTING' and extraction_expires_at<now()
                    for update skip locked
                ), failed_results as (
                    update extraction_result r set status='FAILED', error_code='EXTRACTION_INTERRUPTED',
                        error_message='Extraction reservation expired; explicit retry is required.',
                        updated_at=now(), version=r.version+1
                    from expired e where r.id=e.extraction_token and r.document_id=e.id and r.status='PENDING'
                    returning r.id
                )
                update claim_document d set processing_status='FAILED', extraction_token=null,
                    extraction_expires_at=null, updated_at=now(), version=d.version+1
                from expired e where d.id=e.id
                """);
    }

    @Scheduled(fixedDelay=60000, initialDelay=60000)
    public void scheduledRecovery() {
        try {
            recoverExpired();
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Extraction recovery unavailable ({})", e.getClass().getSimpleName());
        }
    }
}
