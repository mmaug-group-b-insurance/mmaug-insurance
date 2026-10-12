import { Badge } from "@/components/ui/badge";
import type { Severity } from "@/types/finding";

const MAP: Record<Severity, "muted" | "info" | "warning" | "destructive"> = {
  INFO: "muted",
  LOW: "info",
  MEDIUM: "warning",
  HIGH: "destructive",
  CRITICAL: "destructive",
};

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <Badge variant={MAP[severity]} className="uppercase tracking-wide">{severity}</Badge>;
}