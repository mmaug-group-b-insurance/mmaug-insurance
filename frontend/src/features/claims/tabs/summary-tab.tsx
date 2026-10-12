import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Bot, CheckCircle2, RefreshCcw } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { summariesService } from "@/services/summaries";
import { StatementItem } from "@/features/summary/statement-item";
import { FactVsAiLegend } from "@/features/summary/fact-vs-ai-legend";
import { useAuth } from "@/features/auth/use-auth";
import type { Claim } from "@/types/claim";
import type { SummaryStatement } from "@/types/summary";

export function ClaimSummaryTab({ claim }: { claim: Claim }) {
  const [version, setVersion] = useState<number | undefined>(undefined);
  const { hasRole } = useAuth();
  const canView = hasRole(["CLAIMS_OFFICER", "AUDITOR", "ADMIN", "CLAIMANT"]);

  const versionsQuery = useQuery({
    queryKey: ["claim-summary-versions", claim.id],
    queryFn: ({ signal }) => summariesService.versions(claim.id, signal),
    enabled: canView,
    staleTime: 60_000,
  });

  const summaryQuery = useQuery({
    queryKey: ["claim-summary", claim.id, version],
    queryFn: ({ signal }) => summariesService.get(claim.id, version, signal),
    enabled: canView,
    staleTime: 30_000,
    retry: 0,
  });

  if (!canView) {
    return (
      <Alert>
        <AlertTitle>Not available</AlertTitle>
        <AlertDescription>You do not have permission to view the summary.</AlertDescription>
      </Alert>
    );
  }

  if (summaryQuery.isLoading) {
    return <div className="space-y-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-24 w-full" />)}</div>;
  }

  if (summaryQuery.isError) {
    const notFound = (summaryQuery.error as { status?: number })?.status === 404;
    if (notFound) {
      return (
        <EmptyState
          icon={Bot}
          title="No summary generated yet"
          description="An officer must trigger the analysis pipeline before a summary is available."
        />
      );
    }
    return <ErrorAlert error={summaryQuery.error} onRetry={() => summaryQuery.refetch()} />;
  }

  const summary = summaryQuery.data;
  if (!summary) return null;

  const sections = Object.entries(summary.sections ?? {});

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-start justify-between space-y-0">
          <div>
            <CardTitle className="text-base">{summary.headline}</CardTitle>
            <CardDescription>
              Generated {new Date(summary.generatedAt).toLocaleString()} · prompt {summary.promptVersion} · model {summary.llmModel}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            {versionsQuery.data && versionsQuery.data.length > 1 && (
              <Select
                value={String(version ?? summary.versionNo)}
                onValueChange={(v) => setVersion(Number(v))}
              >
                <SelectTrigger className="h-8 w-[120px] text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {versionsQuery.data.map((v) => (
                    <SelectItem key={v.summaryId} value={String(v.versionNo)}>
                      v{v.versionNo}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            <Button variant="ghost" size="icon" onClick={() => summaryQuery.refetch()} aria-label="Refresh">
              <RefreshCcw />
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">v{summary.versionNo}</Badge>
            {summary.overallConfidence && (
              <Badge variant={
                summary.overallConfidence === "HIGH" ? "success" :
                summary.overallConfidence === "MEDIUM" ? "warning" : "destructive"
              }>
                Confidence: {summary.overallConfidence}
              </Badge>
            )}
            {summary.escalationRecommended && (
              <Badge variant="destructive">
                <AlertTriangle className="h-3 w-3" /> Escalation recommended
              </Badge>
            )}
          </div>

          <div className="mt-4">
            <FactVsAiLegend />
          </div>
        </CardContent>
      </Card>

      {summary.escalationRecommended && summary.escalationReasons?.length ? (
        <Alert variant="warning">
          <AlertTriangle />
          <AlertTitle>Escalation recommended</AlertTitle>
          <AlertDescription>
            <ul className="ml-4 list-disc space-y-1 text-sm">
              {summary.escalationReasons.map((r, i) => <li key={i}>{r}</li>)}
            </ul>
          </AlertDescription>
        </Alert>
      ) : null}

      {sections.map(([section, statements]) => (
        <Card key={section}>
          <CardHeader>
            <CardTitle className="text-sm uppercase tracking-wide">{section}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {statements.map((s: SummaryStatement) => <StatementItem key={s.id} statement={s} />)}
          </CardContent>
        </Card>
      ))}

      {summary.uncertaintyNotes && summary.uncertaintyNotes.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm">
              <CheckCircle2 className="h-4 w-4 text-warning" /> Uncertainty notes
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="ml-4 list-disc space-y-1 text-sm text-muted-foreground">
              {summary.uncertaintyNotes.map((u, i) => <li key={i}>{u}</li>)}
            </ul>
          </CardContent>
        </Card>
      )}

      {summary.openFindings?.length > 0 && (
        <Alert variant="warning">
          <AlertTriangle />
          <AlertTitle>Open findings</AlertTitle>
          <AlertDescription>
            {summary.openFindings.length} unresolved finding{summary.openFindings.length === 1 ? "" : "s"} relate to this summary.
          </AlertDescription>
        </Alert>
      )}

      <p className="text-[10px] italic text-muted-foreground">{summary.disclaimer}</p>
    </div>
  );
}