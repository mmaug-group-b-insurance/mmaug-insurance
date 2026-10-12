export interface AuditEvent {
  id: string;
  occurredAt: string;
  actor: string;
  actorRole: string;
  actorType: "USER" | "AGENT" | "SYSTEM";
  action: string;
  outcome: "SUCCESS" | "FAILURE" | "DENIED";
  entityType: string;
  entityId: string;
  claimId?: string | null;
  ipAddress?: string | null;
  correlationId?: string | null;
  details?: Record<string, unknown> | null;
  hash: string;
  prevHash: string;
}

export interface AuditFilters {
  claimId?: string;
  actorUserId?: string;
  action?: string;
  outcome?: string;
  from?: string;
  to?: string;
  correlationId?: string;
  page?: number;
  size?: number;
}