export type DocumentType =
  | "CLAIM_FORM"
  | "POLICE_REPORT"
  | "REPAIR_ESTIMATE"
  | "PHOTO"
  | "INCIDENT_REPORT"
  | "MEDICAL_REPORT"
  | "WITNESS_STATEMENT"
  | "OTHER";

export type ScanStatus = "PENDING" | "CLEAN" | "INFECTED" | "REJECTED";
export type ProcessingStatus = "UPLOADED" | "QUARANTINED" | "EXTRACTING" | "EXTRACTED" | "FAILED";

export interface ClaimDocument {
  id: string;
  claimId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  documentType: DocumentType;
  detectedType?: DocumentType | null;
  scanStatus: ScanStatus;
  processingStatus: ProcessingStatus;
  pageCount?: number | null;
  sha256: string;
  uploadedBy: string;
  uploadedAt: string;
}