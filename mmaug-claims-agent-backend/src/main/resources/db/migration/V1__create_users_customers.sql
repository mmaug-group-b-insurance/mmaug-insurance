-- V1: Users and Customers
-- RFP MMAUG-AIDB-RFP-003
-- PII columns (email, display_name, names, phone, national_id, address) are encrypted
-- at the application layer via AttributeEncryptor. Columns are TEXT to hold ciphertext.

CREATE TABLE customer (
                          id              UUID PRIMARY KEY,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          version         BIGINT NOT NULL DEFAULT 0,
                          customer_number VARCHAR(20) NOT NULL,
                          customer_type   VARCHAR(15) NOT NULL,
                          first_name      VARCHAR(100),
                          last_name       VARCHAR(100),
                          company_name    VARCHAR(200),
                          company_reg_no  VARCHAR(30),
                          date_of_birth   DATE,
                          national_id_no  TEXT,
                          email           TEXT,
                          phone           TEXT,
                          address_line1   TEXT,
                          address_line2   TEXT,
                          locality        VARCHAR(100),
                          postal_code     VARCHAR(15),
                          country         VARCHAR(2) NOT NULL DEFAULT 'MT',
                          CONSTRAINT uq_customer_number UNIQUE (customer_number)
);

CREATE TABLE app_user (
                          id               UUID PRIMARY KEY,
                          created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          version          BIGINT NOT NULL DEFAULT 0,
                          entra_object_id  VARCHAR(64) NOT NULL,
                          email            TEXT,
                          display_name     TEXT,
                          role             VARCHAR(30) NOT NULL,
                          customer_id      UUID,
                          active           BOOLEAN NOT NULL DEFAULT TRUE,
                          last_login_at    TIMESTAMPTZ,
                          CONSTRAINT uq_app_user_entra_object_id UNIQUE (entra_object_id),
                          CONSTRAINT fk_app_user_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE INDEX idx_app_user_customer ON app_user (customer_id);
CREATE INDEX idx_app_user_role ON app_user (role);
CREATE INDEX idx_customer_type ON customer (customer_type);