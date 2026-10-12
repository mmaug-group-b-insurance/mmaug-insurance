import { apiRequest } from "@/lib/api-client";
import type { ClaimDocument, DocumentType } from "@/types/document";

export const documentsService = {
  listForClaim(claimId: string, signal?: AbortSignal) {
    return apiRequest<ClaimDocument[]>(`/claims/${claimId}/documents`, { signal });
  },
  upload(claimId: string, file: File, documentType: DocumentType) {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentType", documentType);
    return apiRequest<ClaimDocument>(`/claims/${claimId}/documents`, { method: "POST", formData: fd });
  },
  remove(documentId: string) {
    return apiRequest<void>(`/documents/${documentId}`, { method: "DELETE" });
  },
  downloadUrl(documentId: string) {
    return `/documents/${documentId}/download`;
  },
};