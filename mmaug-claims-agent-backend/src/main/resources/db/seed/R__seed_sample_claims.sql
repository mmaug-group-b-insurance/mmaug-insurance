-- Repeatable seed: 12 canonical synthetic motor accidental-damage claims for the MVP demo (BRD Section 15).
-- Split: 4 complete (CLM-2026-000001, 000002, 000004, 000005),
--        4 incomplete (CLM-2026-000006, 000007, 000008, 000009),
--        4 conflicting / escalation (CLM-2026-000003, 000010, 000011, 000012).
-- Officer 1: 00000000-0000-0000-0000-000000000001 (default dev/test officer)
-- Officer 2: 00000000-0000-0000-0000-000000000002 (colleague officer for isolation checks)

-- 1. COMPLETE: Maria Borg (Officer 1) - Standard minor junction collision
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000001', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100001', p.id, p.customer_id,
       'Maria Borg', '+35679123451', 'maria.borg@example.mt', 'ABC 123', '2026-09-15', 'Triq Manwel Dimech, Sliema',
       'Minor collision at junction. Front bumper damage. No injuries.', FALSE, FALSE, 800.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100001'
    ON CONFLICT (claim_number) DO NOTHING;

-- 2. COMPLETE: John Camilleri (Officer 2) - Windscreen impact (isolated to Officer 2)
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000002', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'POL-MT-100002', p.id, p.customer_id,
       'John Camilleri', '+35679123452', 'john.camilleri@example.mt', 'DEF 456', '2026-02-20', 'Triq il-Kbira, Mosta',
       'Windscreen cracked by stone from passing truck. No injuries reported.', FALSE, FALSE, 450.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100002'
    ON CONFLICT (claim_number) DO NOTHING;

-- 3. CONFLICTING / ESCALATION: Maria Borg (Officer 1) - High-value collision with third-party & police report
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, police_report_number, estimated_amount,
                   currency, reported_at, submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000003', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100001', p.id, p.customer_id,
       'Maria Borg', '+35679123451', 'maria.borg@example.mt', 'ABC 123', '2026-09-25', 'Triq San Gorg, Valletta',
       'Heavy side-impact accidental damage at crossroads. Police report filed.', TRUE, TRUE, 'PR-2026-09125', 5000.00,
       'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100001'
    ON CONFLICT (claim_number) DO NOTHING;

-- 4. COMPLETE: Clara Galea (Officer 1) - Complete rear-bumper accidental damage
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000004', 'SUBMITTED', 'EMAIL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100006', p.id, p.customer_id,
       'Clara Galea', '+35679123456', 'clara.galea@example.mt', 'MNO 345', '2026-10-04', 'Vjal il-Labour, Naxxar',
       'Low-speed rear bumper impact while stationary at pedestrian crossing.', FALSE, FALSE, 1150.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100006'
    ON CONFLICT (claim_number) DO NOTHING;

-- 5. COMPLETE: Sophie Attard (Officer 2) - Complete roundabout scrape
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000005', 'SUBMITTED', 'PHONE',
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'POL-MT-100008', p.id, p.customer_id,
       'Sophie Attard', '+35679123458', 'sophie.attard@example.mt', 'STU 901', '2026-10-12', 'Triq Dun Karm, Birkirkara',
       'Nearside wing panel scraped against barrier while exiting bypass lane.', FALSE, FALSE, 980.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100008'
    ON CONFLICT (claim_number) DO NOTHING;

-- 6. INCOMPLETE: David Farrugia (Officer 1) - Missing incident location and vehicle registration
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000006', 'SUBMITTED', 'EMAIL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100007', p.id, p.customer_id,
       'David Farrugia', '+35679123457', 'david.farrugia@example.mt', NULL, '2026-10-08', NULL,
       'Front left headlight and fender damaged during parking manoeuvre.', FALSE, FALSE, 1420.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100007'
    ON CONFLICT (claim_number) DO NOTHING;

-- 7. INCOMPLETE: Luke Spiteri (Officer 1) - Injuries reported without police report number
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, police_report_number, estimated_amount,
                   currency, reported_at, submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000007', 'SUBMITTED', 'BRANCH',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100011', p.id, p.customer_id,
       'Luke Spiteri', '+35679123461', 'luke.spiteri@example.mt', 'BCD 890', '2026-10-14', 'Triq l-Imdina, Zebbug',
       'Rear-end impact at traffic lights; driver reported mild whiplash symptoms.', TRUE, TRUE, NULL, 2600.00,
       'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100011'
    ON CONFLICT (claim_number) DO NOTHING;

-- 8. INCOMPLETE: Matthew Azzopardi (Officer 1) - Missing incident description and claimant contact details
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000008', 'DRAFT', 'PHONE',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100013', p.id, p.customer_id,
       'Matthew Azzopardi', NULL, NULL, 'HIJ 246', '2026-10-18', 'Triq Notabile, Attard',
       NULL, FALSE, FALSE, NULL, 'EUR', NOW(), NULL, FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100013'
    ON CONFLICT (claim_number) DO NOTHING;

-- 9. INCOMPLETE: Daniel Schembri (Officer 2) - Missing vehicle registration on broker intake
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000009', 'SUBMITTED', 'BROKER',
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'POL-MT-100016', p.id, p.customer_id,
       'Daniel Schembri', '+35679123466', 'daniel.schembri@example.mt', NULL, '2026-10-19', 'Triq il-Blue Grotto, Zurrieq',
       'Offside door dented after striking roadside bollard in narrow lane.', FALSE, FALSE, 1300.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100016'
    ON CONFLICT (claim_number) DO NOTHING;

-- 10. CONFLICTING: Elena Vella (Officer 1) - Incident date falls outside lapsed policy period (POL-MT-100003)
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000010', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100003', p.id, p.customer_id,
       'Elena Vella', '+35679123453', 'elena.vella@example.mt', 'GHI 789', '2026-10-05', 'Triq it-Torri, Sliema',
       'Front grille and bonnet damaged in rear-end collision on coastal road.', FALSE, FALSE, 2100.00, 'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100003'
    ON CONFLICT (claim_number) DO NOTHING;

-- 11. CONFLICTING: Mark Micallef (Officer 1) - Stated vehicle registration ZZ 9999 conflicts with policy vehicle VWX 234 & high frequency history
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000011', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'POL-MT-100009', p.id, p.customer_id,
       'Mark Micallef', '+35679123459', 'mark.micallef@example.mt', 'ZZ 9999', '2026-10-10', 'Triq l-Istabal, Qormi',
       'Side doors scraped during lane merge; registration on intake differs from policy.', TRUE, FALSE, 3200.00, 'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100009'
    ON CONFLICT (claim_number) DO NOTHING;

-- 12. CONFLICTING: Rachel Grech (Officer 2) - High-value estimate (>10,000 EUR threshold) & late reporting (>14 days)
INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, assigned_officer_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, police_report_number, estimated_amount,
                   currency, reported_at, submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000012', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'POL-MT-100010', p.id, p.customer_id,
       'Rachel Grech', '+35679123460', 'rachel.grech@example.mt', 'YZA 567', '2026-08-01', 'Triq Ross, St Julians',
       'Major front-end structural damage after hitting stone boundary wall.', FALSE, FALSE, 'PR-2026-08001', 14500.00,
       'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100010'
    ON CONFLICT (claim_number) DO NOTHING;
