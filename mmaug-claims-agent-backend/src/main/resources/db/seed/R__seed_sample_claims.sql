-- Repeatable seed: sample claims for the demo.
-- Claimant user IDs are synthetic and do not need to reference real Entra identities in local dev.

INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000001', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000001', 'POL-MT-100001', p.id, p.customer_id,
       'Maria Borg', '+35679123451', 'maria.borg@example.mt', 'ABC 123', '2026-09-15', 'Triq Manwel Dimech, Sliema',
       'Minor collision at junction. Front bumper damage. No injuries.', FALSE, FALSE, 800.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, estimated_amount, currency, reported_at,
                   submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000002', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000002', 'POL-MT-100002', p.id, p.customer_id,
       'John Camilleri', '+35679123452', 'john.camilleri@example.mt', 'DEF 456', '2026-09-20', 'Triq il-Kbira, Mosta',
       'Windscreen cracked by stone from passing truck. No injuries reported.', FALSE, FALSE, 450.00, 'EUR', NOW(), NOW(), FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100002';

INSERT INTO claim (id, claim_number, status, channel, created_by_user_id, policy_number_stated, policy_id, customer_id,
                   claimant_name, claimant_phone, claimant_email, vehicle_registration, incident_date, incident_location,
                   incident_description, third_party_involved, injuries_reported, police_report_number, estimated_amount,
                   currency, reported_at, submitted_at, risk_flag)
SELECT gen_random_uuid(), 'CLM-2026-000003', 'SUBMITTED', 'WEB_PORTAL',
       '00000000-0000-0000-0000-000000000003', 'POL-MT-100001', p.id, p.customer_id,
       'Maria Borg', '+35679123451', 'maria.borg@example.mt', 'ABC 123', '2026-09-25', 'Triq San Ġorġ, Valletta',
       'Vehicle theft overnight. Police report filed. Vehicle recovered damaged.', TRUE, TRUE, 'PR-2026-09125', 5000.00,
       'EUR', NOW(), NOW(), TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100001';