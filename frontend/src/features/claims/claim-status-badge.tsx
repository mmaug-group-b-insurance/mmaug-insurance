import { Badge, type BadgeProps } from "@/components/ui/badge";
import type { ClaimStatus } from "@/types/claim";

const MAP: Record<ClaimStatus, { label: string; variant: BadgeProps["variant"] }> = {
  DRAFT: { label: "Draft", variant: "muted" },
  SUBMITTED: { label: "Submitted", variant: "info" },
  PROCESSING: { label: "Processing", variant: "info" },
  AWAITING_INFORMATION: { label: "Awaiting information", variant: "warning" },
  READY_FOR_REVIEW: { label: "Ready for review", variant: "warning" },
  ESCALATED: { label: "Escalated", variant: "destructive" },
  UNDER_INVESTIGATION: { label: "Under investigation", variant: "info" },
  CLOSED_BY_OFFICER: { label: "Closed by officer", variant: "success" },
};

export function ClaimStatusBadge({ status }: { status: ClaimStatus }) {
  const cfg = MAP[status] ?? { label: status, variant: "muted" as const };
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>;
}