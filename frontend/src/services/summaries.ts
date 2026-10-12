import { apiRequest } from "@/lib/api-client";
import type { Summary, SummaryVersionItem } from "@/types/summary";

export const summariesService = {
  get(claimId: string, version?: number, signal?: AbortSignal) {
    return apiRequest<Summary>(`/claims/${claimId}/summary`, { query: { version }, signal });
  },
  versions(claimId: string, signal?: AbortSignal) {
    return apiRequest<SummaryVersionItem[]>(`/claims/${claimId}/summary/versions`, { signal });
  },
};