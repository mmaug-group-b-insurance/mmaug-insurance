export type ReviewOutcome =
  | "ACKNOWLEDGED"
  | "INFO_REQUESTED"
  | "ESCALATED_TO_SENIOR"
  | "ACCEPTED_FOR_INVESTIGATION"
  | "SUMMARY_REJECTED_REGENERATE";

export interface ReviewEdit {
  statementId: string;
  editedText: string;
  reason: string;
}

export interface Review {
  id: string;
  claimId: string;
  summaryId: string;
  reviewer: string;
  outcome: ReviewOutcome;
  comment: string;
  infoRequested?: string | null;
  edits?: ReviewEdit[];
  reviewedAt: string;
}

export interface CreateReviewInput {
  summaryId: string;
  outcome: ReviewOutcome;
  comment: string;
  infoRequested?: string;
  agreedWithEscalation?: boolean;
  edits?: ReviewEdit[];
}