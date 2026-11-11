-- Repeatable seed: 20 synthetic policies matching the 20 synthetic customers.

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

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100006', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-01-01', '2026-12-31', 490.00, 'EUR', 250.00, 4, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0006'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100007', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-02-01', '2027-01-31', 610.00, 'EUR', 350.00, 2, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0007'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100008', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-01-15', '2027-01-14', 475.00, 'EUR', 250.00, 6, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0008'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100009', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2025-11-01', '2026-10-31', 540.00, 'EUR', 300.00, 1, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0009'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100010', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-03-01', '2027-02-28', 680.00, 'EUR', 400.00, 5, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0010'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100011', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-01-01', '2026-12-31', 420.00, 'EUR', 200.00, 7, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0011'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100012', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2025-07-01', '2026-06-30', 510.00, 'EUR', 250.00, 3, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0012'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100013', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-04-01', '2027-03-31', 460.00, 'EUR', 250.00, 4, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0013'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100014', c.id, 'PRIVATE_MOTOR', 'CANCELLED', '2025-01-01', '2025-08-31', 390.00, 'EUR', 250.00, 0, 'OVERDUE'
FROM customer c WHERE c.customer_number = 'CUS-0014'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100015', c.id, 'FLEET', 'ACTIVE', '2026-01-01', '2026-12-31', 8400.00, 'EUR', 500.00, 6, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0015'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100016', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-01-01', '2026-12-31', 440.00, 'EUR', 250.00, 5, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0016'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100017', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-02-15', '2027-02-14', 505.00, 'EUR', 300.00, 2, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0017'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100018', c.id, 'PRIVATE_MOTOR', 'EXPIRED', '2024-01-01', '2024-12-31', 410.00, 'EUR', 200.00, 3, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0018'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100019', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-01-01', '2026-12-31', 470.00, 'EUR', 250.00, 4, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0019'
    ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy (id, policy_number, customer_id, product_code, status, start_date, end_date, annual_premium, currency, standard_excess, no_claims_bonus_years, payment_status)
SELECT gen_random_uuid(), 'POL-MT-100020', c.id, 'PRIVATE_MOTOR', 'ACTIVE', '2026-05-01', '2027-04-30', 530.00, 'EUR', 300.00, 1, 'PAID'
FROM customer c WHERE c.customer_number = 'CUS-0020'
    ON CONFLICT (policy_number) DO NOTHING;
