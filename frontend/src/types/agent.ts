export type AgentRunStatus =
  | "QUEUED"
  | "RUNNING"
  | "COMPLETED"
  | "COMPLETED_WITH_WARNINGS"
  | "ESCALATED"
  | "FAILED";

export type AgentStepType =
  | "SCAN_CHECK"
  | "OCR"
  | "STRUCTURE"
  | "CONSOLIDATE"
  | "VALIDATE"
  | "TOOL_CALL"
  | "GUARDRAIL"
  | "GENERATE_SUMMARY"
  | "VERIFY_EVIDENCE"
  | "ESCALATION_CHECK";

export type ToolCallStatus = "SUCCESS" | "NOT_FOUND" | "FAILED" | "TIMEOUT" | "DENIED" | "CIRCUIT_OPEN";

export interface AgentStep {
  stepNo: number;
  type: AgentStepType;
  name: string;
  status: "SUCCESS" | "FAILED" | "SKIPPED";
  durationMs?: number | null;
  errorCode?: string | null;
  inputSummary?: unknown;
  outputSummary?: unknown;
  startedAt?: string | null;
}

export interface ToolCall {
  id: string;
  toolName: string;
  status: ToolCallStatus;
  httpStatus?: number | null;
  latencyMs?: number | null;
  retryCount: number;
  arguments: Record<string, unknown>;
  resultEnvelope?: unknown;
  calledAt: string;
  errorCode?: string | null;
}

export interface AgentRun {
  runId: string;
  claimId: string;
  status: AgentRunStatus;
  startedAt: string;
  completedAt?: string | null;
  escalationRequired: boolean;
  warnings?: string[];
  promptVersion: string;
  toolSchemaVersion: string;
  ruleSetVersion: string;
  llmModel: string;
}

export interface AgentRunDetail extends AgentRun {
  steps: AgentStep[];
  toolCalls: ToolCall[];
  tokensIn?: number | null;
  tokensOut?: number | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

export interface AnalyseRequest {
  forceReprocess?: boolean;
  includeDocumentIds?: string[];
}