import { Badge } from "@/components/ui/badge";

export function FactVsAiLegend() {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border bg-muted/20 p-3 text-xs">
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">Legend:</span>
      <Badge variant="info">Retrieved fact</Badge>
      <Badge variant="success">Extracted fact</Badge>
      <Badge variant="muted">AI observation</Badge>
      <Badge variant="warning">Uncertain</Badge>
      <Badge variant="default">Recommended check</Badge>
    </div>
  );
}