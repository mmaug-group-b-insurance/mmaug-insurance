import { apiRequest } from "@/lib/api-client";
import type { Page } from "@/types/common";
import type { AuditEvent, AuditFilters } from "@/types/audit";

export const auditService = {
  list(filters: AuditFilters, signal?: AbortSignal) {
    return apiRequest<Page<AuditEvent>>("/audit-events", { query: { ...filters }, signal });
  },
};