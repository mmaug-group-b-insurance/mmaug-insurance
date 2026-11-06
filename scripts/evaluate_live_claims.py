"""Opt-in synthetic evaluation against the running LOCAL preview. No credentials read.

Requires Pillow for fixture rendering. No reviews/resolutions are submitted.
Each run creates new cases and can incur Azure OCR/model usage. Never auto-retries.
"""
import argparse
import hashlib
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path

DISCREPANCIES = {"CONTRADICTION", "VEHICLE_MISMATCH", "POLICY_INACTIVE_ON_DATE"}


def score(expected, absent, fields, expected_flags, findings):
    actual = {f["fieldName"]: f.get("value") for f in fields}
    present = [{"field": k, "expected": v, "actual": actual.get(k),
                "pass": actual.get(k) == v} for k, v in expected.items()]
    missing = [{"field": k, "actual": actual.get(k), "pass": not actual.get(k)} for k in absent]
    flags = {f["type"] for f in findings if f["type"] in DISCREPANCIES}
    wanted = set(expected_flags)
    return {"presentFieldChecks": present, "absentFieldChecks": missing,
            "truePositiveFlags": len(flags & wanted), "falsePositiveFlags": len(flags - wanted),
            "falseNegativeFlags": len(wanted - flags), "expectedFlags": sorted(wanted),
            "actualFlags": sorted(flags), "flagsExact": flags == wanted}


def citations(summary, extraction, run):
    statements = [s for group in summary.get("sections", {}).values() for s in group]
    tool_ids = {t["id"] for t in run.get("toolCalls", [])}
    fields = {f["fieldName"]: f for f in extraction.get("fields", [])}
    problems = []
    for s in statements:
        if not s.get("evidence"):
            problems.append("Missing evidence: " + s["id"])
        for ref in s.get("evidence", []):
            if ref.get("sourceType") == "DOCUMENT":
                field = fields.get(ref.get("fieldName"), {})
                if (ref.get("documentId") != extraction["documentId"] or
                        ref.get("extractionId") != extraction["extractionId"] or
                        ref.get("page") != field.get("page") or
                        ref.get("snippet") != field.get("value")):
                    problems.append("Document citation mismatch: " + s["id"])
            elif ref.get("sourceType") == "TOOL_RESULT":
                if ref.get("toolCallId") not in tool_ids:
                    problems.append("Tool reference mismatch: " + s["id"])
            else:
                problems.append("Unscored source type: " + str(ref.get("sourceType")))
    return {"statementCount": len(statements), "problems": problems,
            "pass": bool(statements) and not problems}


def request(base, path, body=None, headers=None):
    req = urllib.request.Request(base + path, data=body, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        # Never echo raw response/provider diagnostics into console or reports.
        raise RuntimeError(f"HTTP {error.code} at {path}") from None


def post(base, path, value, headers=None):
    return request(base, path, json.dumps(value).encode(),
                   {"Content-Type": "application/json", **(headers or {})})


def render(case, target):
    from PIL import Image, ImageDraw, ImageFont
    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", case.get("fontSize", 30))
    image = Image.new("RGB", (1600, 1000), "white")
    draw = ImageDraw.Draw(image)
    draw.text((45, 35), "SYNTHETIC EVALUATION ONLY - " + case["id"], font=font, fill="black")
    for index, line in enumerate(case["lines"]):
        draw.text((45, 125 + index * 90), line, font=font, fill="black")
    image.save(target)
    return target.read_bytes()


def upload(base, claim_id, filename, data):
    boundary = "mmaug" + uuid.uuid4().hex
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
            "Content-Type: image/png\r\n\r\n").encode() + data
    body += (f"\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"documentType\"\r\n\r\n"
             f"CLAIM_FORM\r\n--{boundary}--\r\n").encode()
    return request(base, f"/claims/{claim_id}/documents", body,
                   {"Content-Type": "multipart/form-data; boundary=" + boundary})


def aggregate(results):
    scored = [r["scores"] for r in results if "scores" in r]
    checks = [c for s in scored for c in s["presentFieldChecks"]]
    absent = [c for s in scored for c in s["absentFieldChecks"]]
    tp = sum(s["truePositiveFlags"] for s in scored)
    fp = sum(s["falsePositiveFlags"] for s in scored)
    fn = sum(s["falseNegativeFlags"] for s in scored)
    return {"casesAttempted": len(results), "casesScored": len(scored),
            "caseFailures": sum(not r.get("pass", False) for r in results),
            "presentFieldExactMatches": sum(c["pass"] for c in checks), "presentFieldChecks": len(checks),
            "presentFieldExactMatchRate": sum(c["pass"] for c in checks) / len(checks) if checks else None,
            "absentFieldsKeptUnknown": sum(c["pass"] for c in absent), "absentFieldChecks": len(absent),
            "discrepancyTruePositives": tp, "discrepancyFalsePositives": fp, "discrepancyFalseNegatives": fn,
            "discrepancyPrecision": tp / (tp + fp) if tp + fp else None,
            "discrepancyRecall": tp / (tp + fn) if tp + fn else None}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://127.0.0.1:8081/api/v1")
    parser.add_argument("--manifest", type=Path, default=Path(__file__).resolve().parents[1] / "tests/evaluation/live-cases.json")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--execute-live", action="store_true", help="Create synthetic cases and invoke paid providers")
    args = parser.parse_args()
    host = urllib.parse.urlparse(args.base)
    if host.hostname not in {"localhost", "127.0.0.1"} or host.scheme != "http" or host.path != "/api/v1":
        parser.error("Only the local preview /api/v1 is supported")
    manifest_bytes = args.manifest.read_bytes()
    cases = json.loads(manifest_bytes)["cases"]
    if not args.execute_live:
        print(f"Validated dry run: {len(cases)} cases. No API writes/provider calls. Use --execute-live to execute.")
        return
    args.output.mkdir(parents=True, exist_ok=False)
    report = {"startedAt": datetime.now(timezone.utc).isoformat(),
              "manifestSha256": hashlib.sha256(manifest_bytes).hexdigest(),
              "scope": "Developer-authored synthetic regression; not an independent holdout, business sign-off or production readiness",
              "matching": "Exact extracted strings; explicit absent fields; three policy discrepancy types scored as sets per case",
              "humanReview": "Not submitted; manual assessment remains pending", "results": []}
    for case in cases:
        result = {"case": case["id"], "pass": False}
        report["results"].append(result)
        try:
            data = render(case, args.output / (case["id"] + ".png"))
            result["fixtureSha256"] = hashlib.sha256(data).hexdigest()
            claim = post(args.base, "/claims", {
                "policyNumber": "POL-MT-100001", "claimantName": "Synthetic eval " + case["id"],
                "claimantPhone": "+12025550123", "claimantEmail": "evaluation@example.invalid",
                "vehicleRegistration": "ABC 123", "incidentDate": "2026-09-15",
                "incidentLocation": "Synthetic evaluation road", "incidentDescription": "Synthetic motor accidental damage evaluation only",
                "injuriesReported": False, "thirdPartyInvolved": False})
            result.update(claimId=claim["id"], claimNumber=claim["claimNumber"])
            doc = upload(args.base, claim["id"], case["id"] + ".png", data)
            if doc["scanStatus"] != "CLEAN":
                raise RuntimeError("Document not cleared; no analysis invoked")
            start = time.monotonic()
            reserved = post(args.base, f"/claims/{claim['id']}/analyse", {}, {"Idempotency-Key": "eval-" + uuid.uuid4().hex})
            result["analysisSeconds"] = round(time.monotonic() - start, 3)
            run = request(args.base, "/agent-runs/" + reserved["runId"])
            result.update(runId=reserved["runId"], runStatus=run["status"], model=run.get("llmModel"),
                          tokensIn=run.get("tokensIn"), tokensOut=run.get("tokensOut"))
            if run["status"] not in {"COMPLETED", "COMPLETED_WITH_WARNINGS", "ESCALATED"}:
                raise RuntimeError("Run not publishable: " + str(run.get("errorCode")))
            summary = request(args.base, f"/claims/{claim['id']}/summary")
            extraction = request(args.base, f"/documents/{doc['id']}/extraction")
            findings = request(args.base, f"/claims/{claim['id']}/findings")
            (args.output / (case["id"] + "-actual.json")).write_text(json.dumps({
                "run": run, "summary": summary, "extraction": extraction, "findings": findings}, indent=2), encoding="utf-8")
            result["summaryId"] = summary["summaryId"]
            result["scores"] = score(case["expected"], case["absent"], extraction["fields"], case["expectedFlags"], findings)
            result["citations"] = citations(summary, extraction, run)
            result["pass"] = (all(c["pass"] for c in result["scores"]["presentFieldChecks"] + result["scores"]["absentFieldChecks"])
                              and result["scores"]["flagsExact"] and result["citations"]["pass"]
                              and str(run.get("llmModel", "")).startswith("azure-openai/")
                              and extraction["extractorModel"] == "prebuilt-layout")
        except Exception as error:
            result["error"] = str(error) if isinstance(error, RuntimeError) else type(error).__name__
        report["metrics"] = aggregate(report["results"])
        (args.output / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(json.dumps({k: result.get(k) for k in ("case", "claimNumber", "pass", "error")}), flush=True)
        if "error" in result:
            break  # No automatic paid retry or cascading calls after service failure.
    report["completedAllCases"] = len(report["results"]) == len(cases)
    report["finishedAt"] = datetime.now(timezone.utc).isoformat()
    (args.output / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    raise SystemExit(0 if report["completedAllCases"] and report["metrics"]["caseFailures"] == 0 else 1)


if __name__ == "__main__":
    main()
