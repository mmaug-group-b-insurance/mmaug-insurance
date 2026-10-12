import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Activity, Bot, FileText, ClipboardCheck } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { agentRunsService } from "@/services/agent-runs";
import { documentsService } from "@/services/documents";
import { reviewsService } from "@/services/reviews";
import { formatRelativeTime } from "@/lib/utils";
import type { Claim } from "@/types/claim";

export function ClaimActivityTab({ claim }: { claim: Claim }) {
  const runsQuery = useQuery({
    queryKey: ["claim-runs", claim.id],
    queryFn: ({ signal }) => agentRunsService.listForClaim(claim.id, signal),
    staleTime: 15_000,
  });

  const docsQuery = useQuery({
    queryKey: ["claim-documents", claim.id],
    queryFn: ({ signal }) => documentsService.listForClaim(claim.id, signal),
    staleTime: 15_000,
  });

  const reviewsQuery = useQuery({
    queryKey: ["claim-reviews", claim.id],
    queryFn: ({ signal }) => reviewsService.list(claim.id, signal),
    staleTime: 15_000,
  });

  const events = [
    ...(docsQuery.data ?? []).map((d) => ({
      at: d.uploadedAt,
      kind: "document" as const,
      title: `Document uploaded · ${d.originalFilename}`,
      detail: `${d.documentType.replace(/_/g, " ")} · scan ${d.scanStatus} · ${d.processingStatus}`,
    })),
    ...(runsQuery.data ?? []).map((r) => ({
      at: r.startedAt,
      kind: "run" as const,
      title: `Agent run ${r.runId.slice(0, 8)}`,
      detail: `${r.status}${r.escalationRequired ? " · escalation recommended" : ""}`,
      href: `/app/runs/${r.runId}`,
    })),
    ...(reviewsQuery.data ?? []).map((r) => ({
      at: r.reviewedAt,
      kind: "review" as const,
      title: `Review submitted · ${r.outcome}`,
      detail: r.comment.slice(0, 140),
    })),
  ].sort((a, b) => new Date(b.at).getTime() - new Date(a.at).getTime());

  const anyLoading = docsQuery.isLoading || runsQuery.isLoading || reviewsQuery.isLoading;
  const anyError = docsQuery.isError || runsQuery.isError || reviewsQuery.isError;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Activity className="h-4 w-4" /> Activity
        </CardTitle>
        <CardDescription>Documents, agent runs, and reviews for this claim.</CardDescription>
      </CardHeader>
      <CardContent>
        {anyLoading ? (
          <div className="space-y-2">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
        ) : anyError ? (
          <ErrorAlert error={docsQuery.error ?? runsQuery.error ?? reviewsQuery.error} />
        ) : events.length === 0 ? (
          <EmptyState
            icon={Activity}
            title="No activity yet"
            description="Documents, agent runs, and reviews appear here as they happen."
          />
        ) : (
          <ol className="relative space-y-4 border-l pl-6">
            {events.map((e, i) => (
              <li key={i} className="relative">
                <span className="absolute -left-[29px] flex h-5 w-5 items-center justify-center rounded-full border bg-background">
                  {e.kind === "document" ? <FileText className="h-3 w-3" /> :
                   e.kind === "run" ? <Bot className="h-3 w-3" /> :
                   <ClipboardCheck className="h-3 w-3" />}
                </span>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-medium">{e.title}</p>
                  <span className="text-[10px] text-muted-foreground">{formatRelativeTime(e.at)}</span>
                </div>
                <p className="text-xs text-muted-foreground">{e.detail}</p>
                {e.href && (
                  <Link to={e.href} className="mt-1 inline-block text-xs text-primary hover:underline">
                    Open run →
                  </Link>
                )}
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  );
}