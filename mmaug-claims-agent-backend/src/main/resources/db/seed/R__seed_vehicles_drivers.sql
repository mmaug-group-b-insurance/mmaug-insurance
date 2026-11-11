-- Repeatable seed: vehicles and drivers for the 20 seeded policies.
-- Guarded with WHERE NOT EXISTS so repeatable Flyway execution never duplicates child rows.

INSERT INTO vehicle (id, policy_id, registration_number, vin, make, model, manufacture_year, engine_cc, colour, usage_type, market_value)
SELECT gen_random_uuid(), p.id, v.reg, v.vin, v.make, v.model, v.yr, v.cc, v.colour, v.usage_type, v.val
FROM (VALUES
    ('POL-MT-100001', 'ABC 123', 'WVWZZZ1KZAW000001', 'Volkswagen', 'Golf', 2020, 1600, 'Blue', 'SOCIAL', 15000.00),
    ('POL-MT-100002', 'DEF 456', '1HGBH41JXMN000002', 'Toyota', 'Corolla', 2019, 1800, 'Silver', 'SOCIAL', 14000.00),
    ('POL-MT-100003', 'GHI 789', '3VWFE21C04M000003', 'Renault', 'Clio', 2018, 1200, 'Red', 'SOCIAL', 9000.00),
    ('POL-MT-100004', 'FLT 001', 'WBA5A5C50ED000004', 'BMW', '5 Series', 2022, 2000, 'White', 'COMMERCIAL', 45000.00),
    ('POL-MT-100005', 'JKL 012', 'JHMCM56557C000005', 'Honda', 'Civic', 2017, 1500, 'Black', 'SOCIAL', 8500.00),
    ('POL-MT-100006', 'MNO 345', 'VF1RJA00068000006', 'Peugeot', '208', 2021, 1200, 'Grey', 'SOCIAL', 13500.00),
    ('POL-MT-100007', 'PQR 678', 'WF0FXXWPFFM000007', 'Ford', 'Focus', 2020, 1500, 'Blue', 'SOCIAL', 14800.00),
    ('POL-MT-100008', 'STU 901', 'TMBJB7NE4J0000008', 'Skoda', 'Octavia', 2022, 1500, 'White', 'SOCIAL', 18200.00),
    ('POL-MT-100009', 'VWX 234', 'KMHCT41DBEU000009', 'Hyundai', 'i20', 2019, 1250, 'Silver', 'SOCIAL', 11000.00),
    ('POL-MT-100010', 'YZA 567', 'WDD2050401R000010', 'Mercedes', 'C200', 2021, 1991, 'Black', 'SOCIAL', 28500.00),
    ('POL-MT-100011', 'BCD 890', 'VNKKD3D320A000011', 'Toyota', 'Yaris', 2020, 1500, 'Red', 'SOCIAL', 12900.00),
    ('POL-MT-100012', 'EFG 135', 'ZFA3120000J000012', 'Fiat', '500', 2018, 1242, 'White', 'SOCIAL', 9200.00),
    ('POL-MT-100013', 'HIJ 246', 'SJNFAAJ11U2000013', 'Nissan', 'Qashqai', 2021, 1332, 'Grey', 'SOCIAL', 19500.00),
    ('POL-MT-100014', 'KLM 357', 'WAUZZZ8V6KA000014', 'Audi', 'A3', 2019, 1498, 'Black', 'SOCIAL', 17400.00),
    ('POL-MT-100015', 'FLT 002', 'VF3YDZPMFC2000015', 'Peugeot', 'Boxer', 2022, 2179, 'White', 'COMMERCIAL', 31000.00),
    ('POL-MT-100016', 'NOP 468', 'U5YPG814GKL000016', 'Kia', 'Sportage', 2020, 1598, 'Blue', 'SOCIAL', 18000.00),
    ('POL-MT-100017', 'QRS 579', 'JMZBP6HE601000017', 'Mazda', '3', 2021, 1998, 'Red', 'SOCIAL', 17900.00),
    ('POL-MT-100018', 'TUV 680', 'VSSZZZ5FZJR000018', 'Seat', 'Leon', 2018, 1395, 'Silver', 'SOCIAL', 11500.00),
    ('POL-MT-100019', 'WXY 791', 'W0VBD6EC8JG000019', 'Opel', 'Astra', 2020, 1199, 'Grey', 'SOCIAL', 13200.00),
    ('POL-MT-100020', 'ZAB 802', 'YV1XZ15VDL2000020', 'Volvo', 'XC40', 2022, 1969, 'Black', 'SOCIAL', 29000.00)
) AS v(pol_no, reg, vin, make, model, yr, cc, colour, usage_type, val)
JOIN policy p ON p.policy_number = v.pol_no
WHERE NOT EXISTS (
    SELECT 1 FROM vehicle existing WHERE existing.policy_id = p.id AND existing.registration_number = v.reg
);

INSERT INTO policy_driver (id, policy_id, full_name, date_of_birth, licence_number, is_policyholder, is_named_driver)
SELECT gen_random_uuid(), p.id, d.full_name, d.dob::date, d.lic, TRUE, TRUE
FROM (VALUES
    ('POL-MT-100001', 'Maria Borg', '1985-06-15', 'MT-LIC-0001'),
    ('POL-MT-100002', 'John Camilleri', '1979-11-02', 'MT-LIC-0002'),
    ('POL-MT-100003', 'Elena Vella', '1990-03-21', 'MT-LIC-0003'),
    ('POL-MT-100004', 'SynthLogistics Duty Driver', '1984-01-10', 'MT-LIC-0004'),
    ('POL-MT-100005', 'Paul Zammit', '1988-07-30', 'MT-LIC-0005'),
    ('POL-MT-100006', 'Clara Galea', '1992-04-18', 'MT-LIC-0006'),
    ('POL-MT-100007', 'David Farrugia', '1981-09-25', 'MT-LIC-0007'),
    ('POL-MT-100008', 'Sophie Attard', '1987-12-09', 'MT-LIC-0008'),
    ('POL-MT-100009', 'Mark Micallef', '1976-05-14', 'MT-LIC-0009'),
    ('POL-MT-100010', 'Rachel Grech', '1991-08-03', 'MT-LIC-0010'),
    ('POL-MT-100011', 'Luke Spiteri', '1983-02-27', 'MT-LIC-0011'),
    ('POL-MT-100012', 'Anna Cassar', '1994-10-11', 'MT-LIC-0012'),
    ('POL-MT-100013', 'Matthew Azzopardi', '1986-07-19', 'MT-LIC-0013'),
    ('POL-MT-100014', 'Laura Pace', '1993-01-22', 'MT-LIC-0014'),
    ('POL-MT-100015', 'Harbour Courier Driver', '1982-11-30', 'MT-LIC-0015'),
    ('POL-MT-100016', 'Daniel Schembri', '1980-03-08', 'MT-LIC-0016'),
    ('POL-MT-100017', 'Emma Bonnici', '1995-06-29', 'MT-LIC-0017'),
    ('POL-MT-100018', 'Joseph Abela', '1978-09-12', 'MT-LIC-0018'),
    ('POL-MT-100019', 'Sarah Debono', '1989-04-05', 'MT-LIC-0019'),
    ('POL-MT-100020', 'Kevin Caruana', '1984-12-17', 'MT-LIC-0020')
) AS d(pol_no, full_name, dob, lic)
JOIN policy p ON p.policy_number = d.pol_no
WHERE NOT EXISTS (
    SELECT 1 FROM policy_driver existing WHERE existing.policy_id = p.id AND existing.licence_number = d.lic
);
