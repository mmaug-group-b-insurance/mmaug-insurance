-- Repeatable seed: synthetic customers for development and testing.
-- All data is fictional. Loaded only in local/dev/test profiles.

INSERT INTO customer (id, customer_number, customer_type, first_name, last_name, email, phone, locality, postal_code, country)
VALUES
    (gen_random_uuid(), 'CUS-0001', 'INDIVIDUAL', 'Maria', 'Borg', 'maria.borg@example.mt', '+35679123451', 'Sliema', 'SLM 1234', 'MT'),
    (gen_random_uuid(), 'CUS-0002', 'INDIVIDUAL', 'John', 'Camilleri', 'john.camilleri@example.mt', '+35679123452', 'Valletta', 'VLT 1117', 'MT'),
    (gen_random_uuid(), 'CUS-0003', 'INDIVIDUAL', 'Elena', 'Vella', 'elena.vella@example.mt', '+35679123453', 'Sliema', 'SLM 1234', 'MT'),
    (gen_random_uuid(), 'CUS-0004', 'COMMERCIAL', NULL, NULL, 'fleet@synthlogistics.example.mt', '+35679123454', 'Marsa', 'MRS 1500', 'MT'),
    (gen_random_uuid(), 'CUS-0005', 'INDIVIDUAL', 'Paul', 'Zammit', 'paul.zammit@example.mt', '+35679123455', 'Mosta', 'MST 1000', 'MT')
    ON CONFLICT (customer_number) DO NOTHING;