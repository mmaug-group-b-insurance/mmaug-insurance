import { apiRequest } from "@/lib/api-client";
import type { Finding } from "@/types/finding";

export const findingsService = {
  list(claimId: string, params: { type?: string; severity?: string; resolved?: boolean } = {}, signal?: AbortSignal) {
    return apiRequest<Finding[]>(`/claims/${claimId}/findings`, { query: params, signal });
  },
  resolve(findingId: string, resolutionNote: string) {
    return apiRequest<Finding>(`/findings/${findingId}/resolve`, {
      method: "PATCH",
      body: { resolutionNote },
    });
  },
};