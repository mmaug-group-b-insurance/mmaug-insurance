import { apiRequest } from "@/lib/api-client";
import type { AgentRun, AgentRunDetail } from "@/types/agent";

export const agentRunsService = {
  listForClaim(claimId: string, signal?: AbortSignal) {
    return apiRequest<AgentRun[]>(`/claims/${claimId}/agent-runs`, { signal });
  },
  get(runId: string, signal?: AbortSignal) {
    return apiRequest<AgentRunDetail>(`/agent-runs/${runId}`, { signal });
  },
};