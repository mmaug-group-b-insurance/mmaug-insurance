export const CLAIM_TAB_KEYS = [
  "overview",
  "documents",
  "findings",
  "summary",
  "review",
  "activity",
] as const;

export type ClaimTabKey = (typeof CLAIM_TAB_KEYS)[number];

export const CLAIM_TAB_LABELS: Record<ClaimTabKey, string> = {
  overview: "Overview",
  documents: "Documents",
  findings: "Findings",
  summary: "Summary",
  review: "Review",
  activity: "Activity",
};