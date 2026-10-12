export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type Role = "CLAIMANT" | "CLAIMS_OFFICER" | "AUDITOR" | "ADMIN" | "SERVICE_AGENT";