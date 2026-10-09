-- V2: Policies, vehicles, drivers, coverages

CREATE TABLE policy (
                        id                     UUID PRIMARY KEY,
                        created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        version                BIGINT NOT NULL DEFAULT 0,
                        policy_number          VARCHAR(20) NOT NULL,
                        customer_id            UUID NOT NULL,
                        product_code           VARCHAR(30) NOT NULL,
                        status                 VARCHAR(15) NOT NULL,
                        start_date             DATE NOT NULL,
                        end_date               DATE NOT NULL,
                        annual_premium         NUMERIC(12,2),
                        currency               VARCHAR(3) NOT NULL DEFAULT 'EUR',
                        standard_excess        NUMERIC(12,2),
                        no_claims_bonus_years  INT,
                        payment_status         VARCHAR(15),
                        CONSTRAINT uq_policy_number UNIQUE (policy_number),
                        CONSTRAINT fk_policy_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE vehicle (
                         id                   UUID PRIMARY KEY,
                         created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         version              BIGINT NOT NULL DEFAULT 0,
                         policy_id            UUID NOT NULL,
                         registration_number  VARCHAR(15) NOT NULL,
                         vin                  VARCHAR(17),
                         make                 VARCHAR(50),
                         model                VARCHAR(50),
                         manufacture_year     INT,
                         engine_cc            INT,
                         colour               VARCHAR(30),
                         usage_type           VARCHAR(20),
                         market_value         NUMERIC(12,2),
                         CONSTRAINT fk_vehicle_policy FOREIGN KEY (policy_id) REFERENCES policy (id)
);

CREATE TABLE policy_driver (
                               id                   UUID PRIMARY KEY,
                               created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               version              BIGINT NOT NULL DEFAULT 0,
                               policy_id            UUID NOT NULL,
                               full_name            TEXT,
                               date_of_birth        DATE,
                               licence_number       TEXT,
                               licence_issue_date   DATE,
                               is_policyholder      BOOLEAN NOT NULL DEFAULT FALSE,
                               is_named_driver      BOOLEAN NOT NULL DEFAULT FALSE,
                               CONSTRAINT fk_policy_driver_policy FOREIGN KEY (policy_id) REFERENCES policy (id)
);

CREATE TABLE policy_coverage (
                                 id               UUID PRIMARY KEY,
                                 created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 version          BIGINT NOT NULL DEFAULT 0,
                                 policy_id        UUID NOT NULL,
                                 coverage_type    VARCHAR(30) NOT NULL,
                                 included         BOOLEAN NOT NULL DEFAULT FALSE,
                                 limit_amount     NUMERIC(12,2),
                                 excess_amount    NUMERIC(12,2),
                                 exclusions_text  TEXT,
                                 effective_from   DATE,
                                 effective_to     DATE,
                                 CONSTRAINT fk_policy_coverage_policy FOREIGN KEY (policy_id) REFERENCES policy (id)
);

CREATE INDEX idx_policy_customer ON policy (customer_id);
CREATE INDEX idx_policy_status ON policy (status);
CREATE INDEX idx_vehicle_policy ON vehicle (policy_id);
CREATE INDEX idx_vehicle_reg ON vehicle (registration_number);
CREATE INDEX idx_policy_driver_policy ON policy_driver (policy_id);
CREATE INDEX idx_policy_coverage_policy ON policy_coverage (policy_id);