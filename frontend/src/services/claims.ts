import { apiRequest } from "@/lib/api-client";
import type { Page } from "@/types/common";
import type { Claim, ClaimFilters, ClaimListItem, CreateClaimInput } from "@/types/claim";

export const claimsService = {
  list(filters: ClaimFilters, signal?: AbortSignal) {
    return apiRequest<Page<ClaimListItem>>("/claims", { query: { ...filters }, signal });
  },
  get(id: string, signal?: AbortSignal) {
    return apiRequest<Claim>(`/claims/${id}`, { signal });
  },
  create(input: CreateClaimInput) {
    return apiRequest<Claim>("/claims", { method: "POST", body: input });
  },
  update(id: string, input: Partial<CreateClaimInput> & { version: number }) {
    return apiRequest<Claim>(`/claims/${id}`, { method: "PATCH", body: input });
  },
  submit(id: string) {
    return apiRequest<Claim>(`/claims/${id}/submit`, { method: "POST" });
  },
  analyse(id: string, body: { forceReprocess?: boolean; includeDocumentIds?: string[] } = {}) {
    return apiRequest<{ runId: string }>(`/claims/${id}/analyse`, { method: "POST", body });
  },
};