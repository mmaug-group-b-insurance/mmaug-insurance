-- Repeatable seed: prior claims history across synthetic policies.

INSERT INTO historical_claim (id, policy_id, claim_reference, loss_date, loss_type, description, amount_paid, currency, outcome, at_fault)
SELECT gen_random_uuid(), p.id, h.ref, h.loss_date::date, h.loss_type, h.descr, h.amt, 'EUR', h.outcome, h.at_fault
FROM (VALUES
    ('POL-MT-100001', 'HIST-2024-0001', '2024-04-12', 'WINDSCREEN', 'Windscreen chip repair', 180.00, 'SETTLED', FALSE),
    ('POL-MT-100002', 'HIST-2023-0002', '2023-09-05', 'COLLISION', 'Rear-end collision', 2400.00, 'SETTLED', TRUE),
    ('POL-MT-100004', 'HIST-2024-0003', '2024-11-20', 'THEFT', 'Vehicle theft (recovered)', 3500.00, 'SETTLED', FALSE),
    ('POL-MT-100005', 'HIST-2022-0004', '2022-08-14', 'VANDALISM', 'Keyed paintwork', 800.00, 'REPUDIATED', FALSE),
    ('POL-MT-100007', 'HIST-2026-0005', '2026-02-18', 'COLLISION', 'Minor car park scrape on rear bumper', 620.00, 'SETTLED', TRUE),
    ('POL-MT-100009', 'HIST-2026-0006', '2026-01-10', 'COLLISION', 'Wing mirror clipped in narrow street', 310.00, 'SETTLED', FALSE),
    ('POL-MT-100009', 'HIST-2026-0007', '2026-04-22', 'COLLISION', 'Front bumper impact at roundabout', 1450.00, 'SETTLED', TRUE),
    ('POL-MT-100009', 'HIST-2026-0008', '2026-07-09', 'WINDSCREEN', 'Full windscreen replacement', 590.00, 'SETTLED', FALSE),
    ('POL-MT-100012', 'HIST-2025-0009', '2025-12-03', 'COLLISION', 'Low-speed reversing impact', 940.00, 'SETTLED', TRUE)
) AS h(pol_no, ref, loss_date, loss_type, descr, amt, outcome, at_fault)
JOIN policy p ON p.policy_number = h.pol_no
WHERE NOT EXISTS (
    SELECT 1 FROM historical_claim existing WHERE existing.claim_reference = h.ref
);
