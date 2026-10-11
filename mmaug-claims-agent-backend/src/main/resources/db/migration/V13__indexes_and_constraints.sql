-- V13: Composite indexes and cross-cutting constraints

CREATE INDEX idx_claim_status_created ON claim (status, created_at DESC);
CREATE INDEX idx_claim_officer_status ON claim (assigned_officer_id, status);
CREATE INDEX idx_claim_created_by_status ON claim (created_by_user_id, status);
CREATE INDEX idx_claim_risk_flag ON claim (risk_flag) WHERE risk_flag = TRUE;

CREATE INDEX idx_claim_document_claim_scan ON claim_document (claim_id, scan_status);
CREATE INDEX idx_claim_document_claim_processing ON claim_document (claim_id, processing_status);

CREATE INDEX idx_validation_finding_claim_resolved ON validation_finding (claim_id, resolved);
CREATE INDEX idx_validation_finding_severity_resolved ON validation_finding (severity, resolved);

CREATE INDEX idx_agent_run_claim_started ON agent_run (claim_id, started_at DESC);
CREATE INDEX idx_agent_run_status_started ON agent_run (status, started_at DESC);

CREATE INDEX idx_claim_summary_claim_version ON claim_summary (claim_id, version_no DESC);
CREATE INDEX idx_summary_statement_summary_order ON summary_statement (summary_id, order_no);

CREATE INDEX idx_audit_event_action_occurred ON audit_event (action, occurred_at DESC);
CREATE INDEX idx_audit_event_claim_occurred ON audit_event (claim_id, occurred_at DESC);

CREATE INDEX idx_officer_review_claim_reviewed ON officer_review (claim_id, reviewed_at DESC);

-- Partial unique constraint: one active (non-deleted) document per SHA-256 per claim.
CREATE UNIQUE INDEX uq_claim_document_sha_claim
    ON claim_document (claim_id, sha256)
    WHERE deleted_at IS NULL;