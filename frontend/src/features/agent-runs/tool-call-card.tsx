import { Wrench } from "lucide-react";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import type { ToolCall, ToolCallStatus } from "@/types/agent";

const STATUS_TONE: Record<ToolCallStatus, BadgeProps["variant"]> = {
  SUCCESS: "success",
  NOT_FOUND: "muted",
  FAILED: "destructive",
  TIMEOUT: "warning",
  DENIED: "destructive",
  CIRCUIT_OPEN: "destructive",
};

export function ToolCallCard({ toolCall }: { toolCall: ToolCall }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <div className="flex flex-wrap items-center gap-2">
        <Wrench className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-mono text-xs font-medium">{toolCall.toolName}</span>
        <Badge variant={STATUS_TONE[toolCall.status]} className="text-[10px]">{toolCall.status}</Badge>
        {toolCall.httpStatus != null && (
          <Badge variant="outline" className="text-[10px]">HTTP {toolCall.httpStatus}</Badge>
        )}
        {toolCall.latencyMs != null && (
          <span className="font-mono text-[10px] text-muted-foreground">{toolCall.latencyMs} ms</span>
        )}
        {toolCall.retryCount > 0 && (
          <span className="font-mono text-[10px] text-warning">retries {toolCall.retryCount}</span>
        )}
      </div>

      {Object.keys(toolCall.arguments).length > 0 && (
        <div className="mt-3">
          <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Arguments</p>
          <pre className="mt-1 overflow-x-auto rounded-md bg-muted/30 p-2 text-[10px]">
            {JSON.stringify(toolCall.arguments, null, 2)}
          </pre>
        </div>
      )}

      {toolCall.resultEnvelope != null && (
        <div className="mt-3">
          <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Result</p>
          <pre className="mt-1 max-h-60 overflow-auto rounded-md bg-muted/30 p-2 text-[10px]">
            {JSON.stringify(toolCall.resultEnvelope, null, 2)}
          </pre>
        </div>
      )}

      {toolCall.errorCode && (
        <p className="mt-2 text-[10px] text-destructive">Error: {toolCall.errorCode}</p>
      )}
    </div>
  );
}