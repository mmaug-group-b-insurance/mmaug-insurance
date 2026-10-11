-- Repeatable seed: synthetic policies.

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100001', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2025-01-01', '2026-12-31', 450.00, 'EUR', 250.00, 3, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0001'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100002', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2025-03-15', '2026-03-14', 520.00, 'EUR', 300.00, 5, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0002'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100003', c.id, 'PRIVATE_MOTOR', 'LAPSED', '2023-06-01', '2024-05-31', 430.00, 'EUR', 250.00, 0, 'OVERDUE'
FROM customer c WHERE c.customer_number = 'CUS-0003'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100004', c.id, 'FLEET', 'ACTIVE', '2025-01-01', '2026-12-31', 12000.00, 'EUR', 500.00, 8, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0004'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100005', c.id, 'PRIVATE_MOTOR', 'EXPIRED', '2022-01-01', '2022-12-31', 380.00, 'EUR', 200.00, 2, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0005'
    ON CONFLICT (policy_number) DO NOTHING;