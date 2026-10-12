import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, AlertTriangle, Bot, Wrench } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { agentRunsService } from "@/services/agent-runs";
import { AgentRunTimeline } from "./agent-run-timeline";
import { ToolCallCard } from "./tool-call-card";

export function AgentRunPage() {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();

  const query = useQuery({
    queryKey: ["agent-run", runId],
    queryFn: ({ signal }) => agentRunsService.get(runId!, signal),
    enabled: Boolean(runId),
    staleTime: 10_000,
  });

  if (!runId) return null;

  if (query.isLoading) {
    return (
      <>
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-4 h-40 w-full" />
        <Skeleton className="mt-4 h-64 w-full" />
      </>
    );
  }

  if (query.isError) {
    return (
      <>
        <Button variant="ghost" onClick={() => navigate(-1)} className="mb-4">
          <ArrowLeft /> Back
        </Button>
        <ErrorAlert error={query.error} onRetry={() => query.refetch()} title="Could not load run" />
      </>
    );
  }

  const run = query.data!;

  return (
    <>
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="mb-4">
        <ArrowLeft /> Back
      </Button>

      <PageHeader
        title={`Agent run ${run.runId.slice(0, 8)}`}
        description={`Claim ${run.claimId.slice(0, 8)} · started ${new Date(run.startedAt).toLocaleString()}`}
        actions={
          <Badge variant={
            run.status === "FAILED" ? "destructive" :
            run.status === "ESCALATED" ? "warning" :
            run.status === "COMPLETED_WITH_WARNINGS" ? "warning" : "success"
          }>
            {run.status}
          </Badge>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Bot className="h-4 w-4" /> Pipeline steps
              </CardTitle>
              <CardDescription>Ordered execution with durations and errors.</CardDescription>
            </CardHeader>
            <CardContent>
              <AgentRunTimeline steps={run.steps} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Wrench className="h-4 w-4" /> Tool calls
              </CardTitle>
              <CardDescription>Read-only tool invocations.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {run.toolCalls.length === 0 ? (
                <p className="text-xs text-muted-foreground">No tool calls in this run.</p>
              ) : (
                run.toolCalls.map((t) => <ToolCallCard key={t.id} toolCall={t} />)
              )}
            </CardContent>
          </Card>
        </div>

        <aside className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Run metadata</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-xs">
              <Meta label="Status" value={run.status} />
              <Meta label="Escalation required" value={run.escalationRequired ? "Yes" : "No"} />
              <Meta label="Prompt version" value={run.promptVersion} />
              <Meta label="Tool schema version" value={run.toolSchemaVersion} />
              <Meta label="Rule set version" value={run.ruleSetVersion} />
              <Meta label="LLM model" value={run.llmModel} />
              {run.completedAt && <Meta label="Completed" value={new Date(run.completedAt).toLocaleString()} />}
              {run.tokensIn != null && <Meta label="Tokens in" value={String(run.tokensIn)} />}
              {run.tokensOut != null && <Meta label="Tokens out" value={String(run.tokensOut)} />}
            </CardContent>
          </Card>

          {run.warnings && run.warnings.length > 0 && (
            <Alert variant="warning">
              <AlertTriangle />
              <AlertTitle>Warnings</AlertTitle>
              <AlertDescription>
                <ul className="ml-4 list-disc space-y-1 text-xs">
                  {run.warnings.map((w, i) => <li key={i}>{w}</li>)}
                </ul>
              </AlertDescription>
            </Alert>
          )}

          {run.errorMessage && (
            <Alert variant="destructive">
              <AlertTriangle />
              <AlertTitle>{run.errorCode ?? "Error"}</AlertTitle>
              <AlertDescription className="text-xs">{run.errorMessage}</AlertDescription>
            </Alert>
          )}
        </aside>
      </div>
    </>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-mono">{value}</span>
    </div>
  );
}