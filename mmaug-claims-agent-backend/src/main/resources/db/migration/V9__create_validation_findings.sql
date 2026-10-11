-- V9: Validation findings

CREATE TABLE validation_finding (
                                    id                    UUID PRIMARY KEY,
                                    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    version               BIGINT NOT NULL DEFAULT 0,
                                    claim_id              UUID NOT NULL,
                                    agent_run_id          UUID,
                                    finding_type          VARCHAR(30) NOT NULL,
                                    severity              VARCHAR(10) NOT NULL,
                                    origin                VARCHAR(15) NOT NULL,
                                    field_name            VARCHAR(60),
                                    message               VARCHAR(1000),
                                    evidence_refs         JSONB,
                                    resolved              BOOLEAN NOT NULL DEFAULT FALSE,
                                    resolved_by_user_id   UUID,
                                    resolved_at           TIMESTAMPTZ,
                                    resolution_note       VARCHAR(500),
                                    CONSTRAINT fk_validation_finding_claim FOREIGN KEY (claim_id) REFERENCES claim (id),
                                    CONSTRAINT fk_validation_finding_run FOREIGN KEY (agent_run_id) REFERENCES agent_run (id)
);

CREATE INDEX idx_validation_finding_claim ON validation_finding (claim_id);
CREATE INDEX idx_validation_finding_type ON validation_finding (finding_type);
CREATE INDEX idx_validation_finding_severity ON validation_finding (severity);
CREATE INDEX idx_validation_finding_resolved ON validation_finding (claim_id, resolved);