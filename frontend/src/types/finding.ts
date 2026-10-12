export type FindingType =
  | "MISSING_FIELD"
  | "CONTRADICTION"
  | "LOW_CONFIDENCE"
  | "POLICY_NOT_FOUND"
  | "POLICY_INACTIVE_ON_DATE"
  | "COVERAGE_MISMATCH"
  | "VEHICLE_MISMATCH"
  | "DRIVER_NOT_LISTED"
  | "LATE_REPORTING"
  | "HIGH_VALUE"
  | "FREQUENT_CLAIMANT"
  | "INJECTION_SUSPECTED"
  | "TOOL_UNAVAILABLE"
  | "OTHER";

export type Severity = "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type FindingOrigin = "RULE_ENGINE" | "LLM";

export interface EvidenceRef {
  sourceType: "DOCUMENT" | "TOOL_RESULT" | "RULE" | "CLAIM_FORM_FIELD";
  documentId?: string | null;
  documentName?: string | null;
  page?: number | null;
  snippet?: string | null;
  toolCallId?: string | null;
  toolName?: string | null;
  ruleId?: string | null;
  fieldName?: string | null;
}

export interface Finding {
  id: string;
  claimId: string;
  type: FindingType;
  severity: Severity;
  origin: FindingOrigin;
  fieldName?: string | null;
  message: string;
  evidence: EvidenceRef[];
  resolved: boolean;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  resolutionNote?: string | null;
}