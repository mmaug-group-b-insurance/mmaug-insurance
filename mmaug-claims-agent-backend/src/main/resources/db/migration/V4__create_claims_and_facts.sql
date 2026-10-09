-- V4: Claims and claim facts

CREATE TABLE claim (
                       id                    UUID PRIMARY KEY,
                       created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       version               BIGINT NOT NULL DEFAULT 0,
                       claim_number          VARCHAR(20) NOT NULL,
                       status                VARCHAR(25) NOT NULL,
                       channel               VARCHAR(20) NOT NULL,
                       created_by_user_id    UUID NOT NULL,
                       assigned_officer_id   UUID,
                       policy_number_stated  VARCHAR(30) NOT NULL,
                       policy_id             UUID,
                       customer_id           UUID,
                       claimant_name         TEXT,
                       claimant_phone        TEXT,
                       claimant_email        TEXT,
                       vehicle_registration  VARCHAR(15),
                       incident_date         DATE NOT NULL,
                       incident_time         TIME,
                       incident_location     VARCHAR(300),
                       incident_description  TEXT,
                       third_party_involved  BOOLEAN,
                       police_report_number  VARCHAR(40),
                       injuries_reported     BOOLEAN,
                       estimated_amount      NUMERIC(12,2),
                       currency              VARCHAR(3) DEFAULT 'EUR',
                       reported_at           TIMESTAMPTZ,
                       submitted_at          TIMESTAMPTZ,
                       risk_flag             BOOLEAN DEFAULT FALSE,
                       escalation_reason     VARCHAR(500),
                       latest_run_id         UUID,
                       CONSTRAINT uq_claim_number UNIQUE (claim_number),
                       CONSTRAINT fk_claim_policy FOREIGN KEY (policy_id) REFERENCES policy (id),
                       CONSTRAINT fk_claim_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE claim_fact (
                            id                   UUID PRIMARY KEY,
                            created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            version              BIGINT NOT NULL DEFAULT 0,
                            claim_id             UUID NOT NULL,
                            field_name           VARCHAR(60) NOT NULL,
                            value_text           VARCHAR(1000),
                            status               VARCHAR(15) NOT NULL,
                            confidence           NUMERIC(4,3),
                            source_type          VARCHAR(15),
                            source_document_id   UUID,
                            source_page          INT,
                            source_snippet       VARCHAR(500),
                            alternate_values     JSONB,
                            CONSTRAINT fk_claim_fact_claim FOREIGN KEY (claim_id) REFERENCES claim (id)
);

CREATE INDEX idx_claim_status ON claim (status);
CREATE INDEX idx_claim_created_by ON claim (created_by_user_id);
CREATE INDEX idx_claim_assigned_officer ON claim (assigned_officer_id);
CREATE INDEX idx_claim_policy_number ON claim (policy_number_stated);
CREATE INDEX idx_claim_incident_date ON claim (incident_date);
CREATE INDEX idx_claim_fact_claim ON claim_fact (claim_id);
CREATE INDEX idx_claim_fact_field_name ON claim_fact (field_name);