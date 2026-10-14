

import { apiRequest } from "@/lib/api-client";
import type { Page } from "@/types/common";
import type { Claim, ClaimFilters, ClaimListItem, CreateClaimInput } from "@/types/claim";

// Shape returned by the backend's ClaimDetailResponse today:
//   { claim: {...}, openFindingCounts: {...}, latestRunId?, latestSummaryId? }
// We unwrap it here so the rest of the frontend sees a flat Claim.
interface ClaimDetailEnvelope {
  claim: Claim;
  openFindingCounts?: Record<string, number>;
  latestRunId?: string | null;
  latestSummaryId?: string | null;
}

export const claimsService = {
  list(filters: ClaimFilters, signal?: AbortSignal) {
    return apiRequest<Page<ClaimListItem>>("/claims", { query: { ...filters }, signal });
  },

  async get(id: string, signal?: AbortSignal): Promise<Claim> {
    const raw = await apiRequest<ClaimDetailEnvelope | Claim>(`/claims/${id}`, { signal });

    // If the backend is already flat, pass it through unchanged.
    if ("claimNumber" in raw) {
      return raw as Claim;
    }

    // Otherwise unwrap the envelope.
    const inner = raw.claim;
    return {
      ...inner,
      latestRunId: raw.latestRunId ?? inner.latestRunId ?? null,
      latestSummaryId: raw.latestSummaryId ?? inner.latestSummaryId ?? null,
    };
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