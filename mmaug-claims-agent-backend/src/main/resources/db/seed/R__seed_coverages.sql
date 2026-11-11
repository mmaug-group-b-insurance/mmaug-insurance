-- Repeatable seed: coverages for the 20 synthetic policies.

INSERT INTO policy_coverage (id, policy_id, coverage_type, included, limit_amount, excess_amount, exclusions_text, effective_from, effective_to)
SELECT gen_random_uuid(), p.id, c.cov_type, TRUE, c.limit_amt, c.excess_amt, c.exclusions, p.start_date, p.end_date
FROM (VALUES
    ('POL-MT-100001', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Excludes racing and commercial use'),
    ('POL-MT-100001', 'WINDSCREEN', 1500.00, 100.00, 'Chip repair only once per year'),
    ('POL-MT-100002', 'THIRD_PARTY', NULL::numeric, NULL::numeric, 'Standard third-party liabilities'),
    ('POL-MT-100003', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Lapsed policy coverage record'),
    ('POL-MT-100004', 'COMPREHENSIVE', NULL::numeric, 500.00, 'Fleet vehicles only'),
    ('POL-MT-100005', 'THIRD_PARTY', NULL::numeric, NULL::numeric, 'Expired policy coverage record'),
    ('POL-MT-100006', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Excludes track days and commercial hire'),
    ('POL-MT-100006', 'ROADSIDE_ASSISTANCE', 500.00, 0.00, 'Malta and Gozo roadside recovery'),
    ('POL-MT-100007', 'COMPREHENSIVE', NULL::numeric, 350.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100008', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Excludes undeclared drivers under 25'),
    ('POL-MT-100008', 'COURTESY_CAR', 1000.00, 50.00, 'Up to 7 days during approved repair'),
    ('POL-MT-100009', 'COMPREHENSIVE', NULL::numeric, 300.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100010', 'COMPREHENSIVE', NULL::numeric, 400.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100011', 'COMPREHENSIVE', NULL::numeric, 200.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100012', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100013', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100014', 'THIRD_PARTY', NULL::numeric, NULL::numeric, 'Cancelled policy record'),
    ('POL-MT-100015', 'COMPREHENSIVE', NULL::numeric, 500.00, 'Commercial courier fleet terms apply'),
    ('POL-MT-100016', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100017', 'COMPREHENSIVE', NULL::numeric, 300.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100018', 'THIRD_PARTY', NULL::numeric, NULL::numeric, 'Expired policy coverage record'),
    ('POL-MT-100019', 'COMPREHENSIVE', NULL::numeric, 250.00, 'Standard private motor exclusions apply'),
    ('POL-MT-100020', 'COMPREHENSIVE', NULL::numeric, 300.00, 'Standard private motor exclusions apply')
) AS c(pol_no, cov_type, limit_amt, excess_amt, exclusions)
JOIN policy p ON p.policy_number = c.pol_no
WHERE NOT EXISTS (
    SELECT 1 FROM policy_coverage existing WHERE existing.policy_id = p.id AND existing.coverage_type = c.cov_type
);
