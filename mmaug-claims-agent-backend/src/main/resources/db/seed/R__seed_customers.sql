-- Repeatable seed: 20 synthetic customers for development, testing and MVP demonstration.
-- All data is fictional. Loaded only in local/dev/test profiles.

INSERT INTO customer (id, customer_number, customer_type, first_name, last_name, company_name, company_reg_no, email, phone, locality, postal_code, country)
VALUES
    (gen_random_uuid(), 'CUS-0001', 'INDIVIDUAL', 'Maria', 'Borg', NULL, NULL, 'maria.borg@example.mt', '+35679123451', 'Sliema', 'SLM 1234', 'MT'),
    (gen_random_uuid(), 'CUS-0002', 'INDIVIDUAL', 'John', 'Camilleri', NULL, NULL, 'john.camilleri@example.mt', '+35679123452', 'Valletta', 'VLT 1117', 'MT'),
    (gen_random_uuid(), 'CUS-0003', 'INDIVIDUAL', 'Elena', 'Vella', NULL, NULL, 'elena.vella@example.mt', '+35679123453', 'Sliema', 'SLM 1234', 'MT'),
    (gen_random_uuid(), 'CUS-0004', 'COMMERCIAL', NULL, NULL, 'SynthLogistics Malta Ltd', 'C-90004', 'fleet@synthlogistics.example.mt', '+35679123454', 'Marsa', 'MRS 1500', 'MT'),
    (gen_random_uuid(), 'CUS-0005', 'INDIVIDUAL', 'Paul', 'Zammit', NULL, NULL, 'paul.zammit@example.mt', '+35679123455', 'Mosta', 'MST 1000', 'MT'),
    (gen_random_uuid(), 'CUS-0006', 'INDIVIDUAL', 'Clara', 'Galea', NULL, NULL, 'clara.galea@example.mt', '+35679123456', 'Birkirkara', 'BKR 4010', 'MT'),
    (gen_random_uuid(), 'CUS-0007', 'INDIVIDUAL', 'David', 'Farrugia', NULL, NULL, 'david.farrugia@example.mt', '+35679123457', 'Rabat', 'RBT 1200', 'MT'),
    (gen_random_uuid(), 'CUS-0008', 'INDIVIDUAL', 'Sophie', 'Attard', NULL, NULL, 'sophie.attard@example.mt', '+35679123458', 'Naxxar', 'NXR 2030', 'MT'),
    (gen_random_uuid(), 'CUS-0009', 'INDIVIDUAL', 'Mark', 'Micallef', NULL, NULL, 'mark.micallef@example.mt', '+35679123459', 'Qormi', 'QRM 3100', 'MT'),
    (gen_random_uuid(), 'CUS-0010', 'INDIVIDUAL', 'Rachel', 'Grech', NULL, NULL, 'rachel.grech@example.mt', '+35679123460', 'St Julians', 'STJ 1050', 'MT'),
    (gen_random_uuid(), 'CUS-0011', 'INDIVIDUAL', 'Luke', 'Spiteri', NULL, NULL, 'luke.spiteri@example.mt', '+35679123461', 'Zebbug', 'ZBG 1400', 'MT'),
    (gen_random_uuid(), 'CUS-0012', 'INDIVIDUAL', 'Anna', 'Cassar', NULL, NULL, 'anna.cassar@example.mt', '+35679123462', 'Swieqi', 'SWQ 2100', 'MT'),
    (gen_random_uuid(), 'CUS-0013', 'INDIVIDUAL', 'Matthew', 'Azzopardi', NULL, NULL, 'matthew.azzopardi@example.mt', '+35679123463', 'Attard', 'ATD 1700', 'MT'),
    (gen_random_uuid(), 'CUS-0014', 'INDIVIDUAL', 'Laura', 'Pace', NULL, NULL, 'laura.pace@example.mt', '+35679123464', 'Msida', 'MSD 1800', 'MT'),
    (gen_random_uuid(), 'CUS-0015', 'COMMERCIAL', NULL, NULL, 'Harbour Courier Services Ltd', 'C-90015', 'ops@harbourcourier.example.mt', '+35679123465', 'Paola', 'PLA 1900', 'MT'),
    (gen_random_uuid(), 'CUS-0016', 'INDIVIDUAL', 'Daniel', 'Schembri', NULL, NULL, 'daniel.schembri@example.mt', '+35679123466', 'Zurrieq', 'ZRQ 1100', 'MT'),
    (gen_random_uuid(), 'CUS-0017', 'INDIVIDUAL', 'Emma', 'Bonnici', NULL, NULL, 'emma.bonnici@example.mt', '+35679123467', 'San Gwann', 'SGN 1300', 'MT'),
    (gen_random_uuid(), 'CUS-0018', 'INDIVIDUAL', 'Joseph', 'Abela', NULL, NULL, 'joseph.abela@example.mt', '+35679123468', 'Mellieha', 'MLH 2200', 'MT'),
    (gen_random_uuid(), 'CUS-0019', 'INDIVIDUAL', 'Sarah', 'Debono', NULL, NULL, 'sarah.debono@example.mt', '+35679123469', 'Victoria', 'VCT 1010', 'MT'),
    (gen_random_uuid(), 'CUS-0020', 'INDIVIDUAL', 'Kevin', 'Caruana', NULL, NULL, 'kevin.caruana@example.mt', '+35679123470', 'Fgura', 'FGR 1600', 'MT')
    ON CONFLICT (customer_number) DO NOTHING;
