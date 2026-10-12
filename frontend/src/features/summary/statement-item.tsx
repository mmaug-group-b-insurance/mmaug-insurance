import { StatementTypeTag } from "./statement-type-tag";
import { EvidenceChip } from "./evidence-chip";
import type { SummaryStatement } from "@/types/summary";

export function StatementItem({ statement }: { statement: SummaryStatement }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="flex flex-wrap items-center gap-2">
        <StatementTypeTag type={statement.type} />
        {statement.confidence != null && (
          <span className="font-mono text-[10px] text-muted-foreground">
            confidence {statement.confidence.toFixed(2)}
          </span>
        )}
      </div>
      <p className="mt-2 whitespace-pre-wrap text-sm">{statement.text}</p>
      {statement.evidence.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {statement.evidence.map((e, i) => <EvidenceChip key={i} evidence={e} />)}
        </div>
      )}
    </div>
  );
}