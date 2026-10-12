import { useState } from "react";
import { ChevronDown, ChevronRight, CheckCircle2, XCircle, SkipForward } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { AgentStep } from "@/types/agent";

export function AgentRunTimeline({ steps }: { steps: AgentStep[] }) {
  if (steps.length === 0) {
    return <p className="text-xs text-muted-foreground">No steps recorded.</p>;
  }

  return (
    <ol className="space-y-2">
      {steps.map((step) => <TimelineStep key={step.stepNo} step={step} />)}
    </ol>
  );
}

function TimelineStep({ step }: { step: AgentStep }) {
  const [open, setOpen] = useState(false);

  const Icon =
    step.status === "SUCCESS" ? CheckCircle2 :
    step.status === "FAILED" ? XCircle : SkipForward;

  const tone =
    step.status === "SUCCESS" ? "text-success" :
    step.status === "FAILED" ? "text-destructive" : "text-muted-foreground";

  return (
    <li className="rounded-md border bg-card">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center gap-3 p-3 text-left"
        aria-expanded={open}
      >
        {open ? <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" /> : <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />}
        <Icon className={cn("h-4 w-4 shrink-0", tone)} />
        <span className="flex-1 text-sm">
          <span className="font-medium">{step.name}</span>
          <Badge variant="outline" className="ml-2 font-mono text-[10px]">{step.type}</Badge>
        </span>
        {step.durationMs != null && (
          <span className="font-mono text-[10px] text-muted-foreground">{step.durationMs} ms</span>
        )}
      </button>
      {open && (
        <div className="border-t bg-muted/20 p-3 text-xs">
          <div className="grid gap-2 sm:grid-cols-2">
            <div>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Step no</p>
              <p className="font-mono">{step.stepNo}</p>
            </div>
            <div>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Status</p>
              <p className="font-mono">{step.status}</p>
            </div>
            {step.errorCode && (
              <div>
                <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Error code</p>
                <p className="font-mono text-destructive">{step.errorCode}</p>
              </div>
            )}
          </div>
          {step.inputSummary != null && (
            <div className="mt-3">
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Input summary</p>
              <pre className="mt-1 overflow-x-auto rounded-md bg-background p-2 text-[10px]">
                {typeof step.inputSummary === "string" ? step.inputSummary : JSON.stringify(step.inputSummary, null, 2)}
              </pre>
            </div>
          )}
          {step.outputSummary != null && (
            <div className="mt-3">
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Output summary</p>
              <pre className="mt-1 overflow-x-auto rounded-md bg-background p-2 text-[10px]">
                {typeof step.outputSummary === "string" ? step.outputSummary : JSON.stringify(step.outputSummary, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </li>
  );
}