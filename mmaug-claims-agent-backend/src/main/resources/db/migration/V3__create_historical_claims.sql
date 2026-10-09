-- V3: Historical claims (prior claims from legacy system)

CREATE TABLE historical_claim (
                                  id                UUID PRIMARY KEY,
                                  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  version           BIGINT NOT NULL DEFAULT 0,
                                  policy_id         UUID NOT NULL,
                                  claim_reference   VARCHAR(25) NOT NULL,
                                  loss_date         DATE NOT NULL,
                                  loss_type         VARCHAR(30) NOT NULL,
                                  description       VARCHAR(500),
                                  amount_paid       NUMERIC(12,2),
                                  currency          VARCHAR(3) DEFAULT 'EUR',
                                  outcome           VARCHAR(20),
                                  at_fault          BOOLEAN,
                                  CONSTRAINT fk_historical_claim_policy FOREIGN KEY (policy_id) REFERENCES policy (id)
);

CREATE INDEX idx_historical_claim_policy ON historical_claim (policy_id);
CREATE INDEX idx_historical_claim_loss_date ON historical_claim (loss_date);