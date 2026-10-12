export type ClaimStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "PROCESSING"
  | "AWAITING_INFORMATION"
  | "READY_FOR_REVIEW"
  | "ESCALATED"
  | "UNDER_INVESTIGATION"
  | "CLOSED_BY_OFFICER";

export type ClaimChannel = "WEB_PORTAL" | "EMAIL" | "BRANCH" | "PHONE" | "BROKER";

export interface Claim {
  id: string;
  claimNumber: string;
  status: ClaimStatus;
  channel: ClaimChannel;
  policyNumberStated: string;
  policyId?: string | null;
  claimantName: string;
  claimantPhone?: string;
  claimantEmail?: string;
  vehicleRegistration: string;
  incidentDate: string;
  incidentTime?: string | null;
  incidentLocation: string;
  incidentDescription: string;
  thirdPartyInvolved: boolean;
  policeReportNumber?: string | null;
  injuriesReported: boolean;
  estimatedAmount?: number | null;
  currency: string;
  riskFlag: boolean;
  escalationReason?: string | null;
  createdAt: string;
  submittedAt?: string | null;
  updatedAt: string;
  version: number;
  latestRunId?: string | null;
  latestSummaryId?: string | null;
}

export interface ClaimListItem {
  id: string;
  claimNumber: string;
  status: ClaimStatus;
  claimantName: string;
  policyNumberStated: string;
  incidentDate: string;
  riskFlag: boolean;
  documentCount: number;
  openFindingCount: number;
  createdAt: string;
}

export interface ClaimFilters {
  status?: ClaimStatus;
  claimNumber?: string;
  policyNumber?: string;
  claimantName?: string;
  riskFlag?: boolean;
  dateFrom?: string;
  dateTo?: string;
  assignedToMe?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateClaimInput {
  policyNumber: string;
  claimantName: string;
  claimantPhone: string;
  claimantEmail: string;
  vehicleRegistration: string;
  incidentDate: string;
  incidentTime?: string;
  incidentLocation: string;
  incidentDescription: string;
  thirdPartyInvolved: boolean;
  policeReportNumber?: string;
  injuriesReported: boolean;
  estimatedAmount?: number;
  channel?: ClaimChannel;
}