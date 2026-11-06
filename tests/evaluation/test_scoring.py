"""Verify evaluation cannot silently count wrong facts/citations as a pass."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("evaluation", Path(__file__).resolve().parents[2] / "scripts/evaluate_live_claims.py")
evaluation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evaluation)


class ScoringTest(unittest.TestCase):
    def test_wrong_value_and_fabricated_missing_field_fail(self):
        result = evaluation.score({"policyNumber": "A"}, ["incidentDate"],
                                  [{"fieldName": "policyNumber", "value": "B"},
                                   {"fieldName": "incidentDate", "value": "1900-01-01"}], [], [])
        self.assertFalse(result["presentFieldChecks"][0]["pass"])
        self.assertFalse(result["absentFieldChecks"][0]["pass"])

    def test_false_positive_and_negative_flags_are_separate(self):
        result = evaluation.score({}, [], [], ["CONTRADICTION"], [{"type": "VEHICLE_MISMATCH"}])
        self.assertEqual((result["truePositiveFlags"], result["falsePositiveFlags"], result["falseNegativeFlags"]), (0, 1, 1))
        self.assertFalse(result["flagsExact"])

    def test_missing_citation_and_empty_summary_fail(self):
        self.assertFalse(evaluation.citations({"sections": {}}, {}, {})["pass"])
        self.assertFalse(evaluation.citations({"sections": {"A": [{"id": "s", "evidence": []}]}}, {}, {})["pass"])

    def test_wrong_extraction_cannot_pass_reference_check(self):
        extraction = {"documentId": "d", "extractionId": "e", "fields": [{"fieldName": "policyNumber", "page": 1, "value": "A"}]}
        ref = {"sourceType": "DOCUMENT", "documentId": "d", "extractionId": "wrong", "fieldName": "policyNumber", "page": 1, "snippet": "A"}
        self.assertFalse(evaluation.citations({"sections": {"A": [{"id": "s", "evidence": [ref]}]}}, extraction, {})["pass"])

    def test_no_denominator_does_not_report_perfect_accuracy(self):
        result = evaluation.aggregate([{"pass": False, "error": "provider unavailable"}])
        self.assertIsNone(result["presentFieldExactMatchRate"])
        self.assertEqual(result["caseFailures"], 1)


if __name__ == "__main__":
    unittest.main()
