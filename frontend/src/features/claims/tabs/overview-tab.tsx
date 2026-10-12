import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { FileText, AlertTriangle, Activity, Bot } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { ErrorAlert } from "@/components/ui/error-alert";
import { documentsService } from "@/services/documents";
import { findingsService } from "@/services/findings";
import { agentRunsService } from "@/services/agent-runs";
import { ClaimStatusBadge } from "../claim-status-badge";
import { formatRelativeTime } from "@/lib/utils";
import type { Claim } from "@/types/claim";

export function ClaimOverviewTab({ claim }: { claim: Claim }) {
  const docs = useQuery({
    queryKey: ["claim-documents", claim.id],
    queryFn: ({ signal }) => documentsService.listForClaim(claim.id, signal),
    staleTime: 30_000,
  });

  const findings = useQuery({
    queryKey: ["claim-findings", claim.id],
    queryFn: ({ signal }) => findingsService.list(claim.id, { resolved: false }, signal),
    staleTime: 30_000,
  });

  const runs = useQuery({
    queryKey: ["claim-runs", claim.id],
    queryFn: ({ signal }) => agentRunsService.listForClaim(claim.id, signal),
    staleTime: 30_000,
  });

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Claim information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="Incident description">{claim.incidentDescription}</Row>
          <Row label="Incident location">{claim.incidentLocation}</Row>
          <Row label="Third party involved">{claim.thirdPartyInvolved ? "Yes" : "No"}</Row>
          <Row label="Injuries reported">{claim.injuriesReported ? "Yes" : "No"}</Row>
          {claim.policeReportNumber && <Row label="Police report number">{claim.policeReportNumber}</Row>}
          {claim.incidentTime && <Row label="Incident time">{claim.incidentTime}</Row>}
        </CardContent>
      </Card>

      <div className="space-y-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <FileText className="h-4 w-4" /> Documents
              </CardTitle>
              <CardDescription>Attached to this claim.</CardDescription>
            </div>
            <Link to={`/app/claims/${claim.id}/documents`} className="text-xs text-primary hover:underline">
              View
            </Link>
          </CardHeader>
          <CardContent>
            {docs.isLoading ? (
              <Skeleton className="h-8 w-full" />
            ) : docs.isError ? (
              <ErrorAlert error={docs.error} onRetry={() => docs.refetch()} />
            ) : (
              <p className="text-2xl font-semibold">{docs.data?.length ?? 0}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <AlertTriangle className="h-4 w-4" /> Open findings
              </CardTitle>
              <CardDescription>Unresolved issues requiring attention.</CardDescription>
            </div>
            <Link to={`/app/claims/${claim.id}/findings`} className="text-xs text-primary hover:underline">
              View
            </Link>
          </CardHeader>
          <CardContent>
            {findings.isLoading ? (
              <Skeleton className="h-8 w-full" />
            ) : findings.isError ? (
              <ErrorAlert error={findings.error} onRetry={() => findings.refetch()} />
            ) : (
              <p className="text-2xl font-semibold">{findings.data?.length ?? 0}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <Activity className="h-4 w-4" /> Agent runs
              </CardTitle>
              <CardDescription>Pipeline executions for this claim.</CardDescription>
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            {runs.isLoading ? (
              <Skeleton className="h-8 w-full" />
            ) : runs.isError ? (
              <ErrorAlert error={runs.error} onRetry={() => runs.refetch()} />
            ) : (runs.data?.length ?? 0) === 0 ? (
              <p className="text-xs text-muted-foreground">No runs yet.</p>
            ) : (
              <ul className="space-y-2">
                {runs.data!.slice(0, 3).map((r) => (
                  <li key={r.runId}>
                    <Link
                      to={`/app/runs/${r.runId}`}
                      className="flex items-center justify-between rounded-md border p-2 text-xs transition-colors hover:bg-accent"
                    >
                      <span className="font-mono">{r.runId.slice(0, 8)}</span>
                      <Badge variant={r.status === "FAILED" ? "destructive" : r.status === "ESCALATED" ? "warning" : "muted"}>
                        {r.status}
                      </Badge>
                      <span className="text-muted-foreground">{formatRelativeTime(r.startedAt)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[160px_1fr] gap-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <div className="text-sm">{children}</div>
    </div>
  );
}