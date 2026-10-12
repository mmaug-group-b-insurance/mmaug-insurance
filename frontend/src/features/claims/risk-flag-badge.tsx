import { AlertTriangle } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export function RiskFlagBadge({ show }: { show: boolean }) {
  if (!show) return <span className="text-xs text-muted-foreground">—</span>;
  return (
    <Badge variant="destructive">
      <AlertTriangle className="h-3 w-3" /> Risk
    </Badge>
  );
}