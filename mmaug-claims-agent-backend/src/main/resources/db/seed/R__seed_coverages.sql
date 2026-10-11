-- Repeatable seed: coverages.

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, 'COMPREHENSIVE', TRUE, NULL, 250.00, 'Excludes racing and commercial use', '2025-01-01', '2026-12-31'
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, 'WINDSCREEN', TRUE, 1500.00, 100.00, 'Chip repair only once per year', '2025-01-01', '2026-12-31'
FROM policy p WHERE p.policy_number = 'POL-MT-100001';

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, 'THIRD_PARTY', TRUE, NULL, NULL, 'Standard third-party liabilities', '2025-03-15', '2026-03-14'
FROM policy p WHERE p.policy_number = 'POL-MT-100002';

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, 'COMPREHENSIVE', TRUE, NULL, 500.00, 'Fleet vehicles only', '2025-01-01', '2026-12-31'
FROM policy p WHERE p.policy_number = 'POL-MT-100004';

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, 'THIRD_PARTY', TRUE, NULL, NULL, NULL, '2022-01-01', '2022-12-31'
FROM policy p WHERE p.policy_number = 'POL-MT-100005';