-- Repeatable seed: prior claims.

INSERT INTO historical_claim (id, policy_id, claim_reference, loss_date, loss_type, description, amount_paid, currency, outcome, at_fault)
SELECT gen_random_uuid(), p.id, 'HIST-2024-0001', '2024-04-12', 'WINDSCREEN', 'Windscreen chip repair', 180.00, 'EUR', 'SETTLED', FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO historical_claim (id, policy_id, claim_reference, loss_date, loss_type, description, amount_paid, currency, outcome, at_fault)
SELECT gen_random_uuid(), p.id, 'HIST-2023-0002', '2023-09-05', 'COLLISION', 'Rear-end collision', 2400.00, 'EUR', 'SETTLED', TRUE
FROM policy p WHERE p.policy_number = 'POL-MT-100002';

INSERT INTO historical_claim (id, policy_id, claim_reference, loss_date, loss_type, description, amount_paid, currency, outcome, at_fault)
SELECT gen_random_uuid(), p.id, 'HIST-2024-0003', '2024-11-20', 'THEFT', 'Vehicle theft (recovered)', 3500.00, 'EUR', 'SETTLED', FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100004';

INSERT INTO historical_claim (id, policy_id, claim_reference, loss_date, loss_type, description, amount_paid, currency, outcome, at_fault)
SELECT gen_random_uuid(), p.id, 'HIST-2022-0004', '2022-08-14', 'VANDALISM', 'Keyed paintwork', 800.00, 'EUR', 'REPUDIATED', FALSE
FROM policy p WHERE p.policy_number = 'POL-MT-100005';