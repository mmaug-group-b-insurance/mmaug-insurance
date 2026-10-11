-- Repeatable seed: vehicles and drivers for the seeded policies.

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, 'ABC 123', 'WVWZZZ1KZAW000001', 'Volkswagen', 'Golf', 2020, 1600, 'Blue', 'SOCIAL', 15000.00
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, 'DEF 456', '1HGBH41JXMN000002', 'Toyota', 'Corolla', 2019, 1800, 'Silver', 'SOCIAL', 14000.00
FROM policy p WHERE p.policy_number = 'POL-MT-100002';

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, 'GHI 789', '3VWFE21C04M000003', 'Renault', 'Clio', 2018, 1200, 'Red', 'SOCIAL', 9000.00
FROM policy p WHERE p.policy_number = 'POL-MT-100003';

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, 'FLT 001', 'WBA5A5C50ED000004', 'BMW', '5 Series', 2022, 2000, 'White', 'COMMERCIAL', 45000.00
FROM policy p WHERE p.policy_number = 'POL-MT-100004';

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, 'JKL 012', 'JHMCM56557C000005', 'Honda', 'Civic', 2017, 1500, 'Black', 'SOCIAL', 8500.00
FROM policy p WHERE p.policy_number = 'POL-MT-100005';

INSERT INTO policy_driver (id, policy_id, full_name, date_of_birth, licence_number, is_policyholder, is_named_driver)
SELECT gen_random_uuid(), p.id, 'Maria Borg', '1985-06-15', 'MT-LIC-0001', TRUE, TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO policy_driver (id, policy_id, full_name, date_of_birth, licence_number, is_policyholder, is_named_driver)
SELECT gen_random_uuid(), p.id, 'John Camilleri', '1979-11-02', 'MT-LIC-0002', TRUE, TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100002';

INSERT INTO policy_driver (id, policy_id, full_name, date_of_birth, licence_number, is_policyholder, is_named_driver)
SELECT gen_random_uuid(), p.id, 'Elena Vella', '1990-03-21', 'MT-LIC-0003', TRUE, TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100003';

INSERT INTO policy_driver (id, policy_id, full_name, date_of_birth, licence_number, is_policyholder, is_named_driver)
SELECT gen_random_uuid(), p.id, 'Paul Zammit', '1988-07-30', 'MT-LIC-0004', TRUE, TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100005';