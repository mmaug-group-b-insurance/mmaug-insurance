import type { Finding } from "./finding";

export type StatementType =
  | "RETRIEVED_FACT"
  | "EXTRACTED_FACT"
  | "AI_OBSERVATION"
  | "UNCERTAIN"
  | "RECOMMENDED_CHECK";

export interface SummaryStatement {
  id: string;
  orderNo: number;
  section: string;
  type: StatementType;
  text: string;
  confidence?: number | null;
  evidence: import("./finding").EvidenceRef[];
}

export interface Summary {
  summaryId: string;
  claimId: string;
  versionNo: number;
  headline: string;
  overallConfidence?: "LOW" | "MEDIUM" | "HIGH" | null;
  escalationRecommended: boolean;
  escalationReasons?: string[];
  uncertaintyNotes?: string[];
  disclaimer: string;
  generatedAt: string;
  promptVersion: string;
  llmModel: string;
  sections: Record<string, SummaryStatement[]>;
  openFindings: Finding[];
}

export interface SummaryVersionItem {
  summaryId: string;
  versionNo: number;
  generatedAt: string;
  headline: string;
}