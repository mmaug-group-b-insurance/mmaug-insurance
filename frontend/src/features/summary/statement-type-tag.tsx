import { Badge, type BadgeProps } from "@/components/ui/badge";
import type { StatementType } from "@/types/summary";

const MAP: Record<StatementType, { label: string; variant: BadgeProps["variant"] }> = {
  RETRIEVED_FACT: { label: "Retrieved fact", variant: "info" },
  EXTRACTED_FACT: { label: "Extracted fact", variant: "success" },
  AI_OBSERVATION: { label: "AI observation", variant: "muted" },
  UNCERTAIN: { label: "Uncertain", variant: "warning" },
  RECOMMENDED_CHECK: { label: "Recommended check", variant: "default" },
};

export function StatementTypeTag({ type }: { type: StatementType }) {
  const cfg = MAP[type];
  return <Badge variant={cfg.variant} className="text-[10px]">{cfg.label}</Badge>;
}