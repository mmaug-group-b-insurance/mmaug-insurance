-- V12: Retention policies

CREATE TABLE retention_policy (
                                  id                 UUID PRIMARY KEY,
                                  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  version            BIGINT NOT NULL DEFAULT 0,
                                  entity_type        VARCHAR(30) NOT NULL,
                                  retention_days     INT NOT NULL,
                                  action_on_expiry   VARCHAR(15) NOT NULL,
                                  legal_basis        VARCHAR(200),
                                  active             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_retention_policy_entity ON retention_policy (entity_type);
CREATE INDEX idx_retention_policy_active ON retention_policy (active);

-- Seed default retention policies.
-- Values are implementation assumptions pending confirmation of MMAUG's data-protection policy.
INSERT INTO retention_policy (id, entity_type, retention_days, action_on_expiry, legal_basis, active)
VALUES
    (gen_random_uuid(), 'DOCUMENT',    2555, 'DELETE',     'Insurance claims record retention (7 years)', TRUE),
    (gen_random_uuid(), 'EXTRACTION',  2555, 'DELETE',     'Insurance claims record retention (7 years)', TRUE),
    (gen_random_uuid(), 'SUMMARY',     2555, 'DELETE',     'Insurance claims record retention (7 years)', TRUE),
    (gen_random_uuid(), 'AGENT_RUN',   2555, 'DELETE',     'Insurance claims record retention (7 years)', TRUE),
    (gen_random_uuid(), 'AUDIT_EVENT', 3650, 'ANONYMISE',  'Regulatory audit retention (10 years)',       TRUE);