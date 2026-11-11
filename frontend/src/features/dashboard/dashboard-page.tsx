import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Activity, AlertTriangle, ArrowRight, ClipboardCheck, FileText, PlusCircle, TrendingUp
} from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { claimsService } from "@/services/claims";
import { useAuth } from "@/features/auth/use-auth";
import { ClaimStatusBadge } from "@/features/claims/claim-status-badge";
import { formatRelativeTime } from "@/lib/utils";
import type { ClaimListItem, ClaimStatus } from "@/types/claim";

const STATUS_ORDER: ClaimStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "PROCESSING",
  "AWAITING_INFORMATION",
  "READY_FOR_REVIEW",
  "ESCALATED",
  "UNDER_INVESTIGATION",
  "CLOSED_BY_OFFICER",
];

export function DashboardPage() {
  const { hasRole } = useAuth();
  const isOfficer = hasRole(["CLAIMS_OFFICER"]);
  const isClaimant = hasRole(["CLAIMANT"]);

  const recentQuery = useQuery({
    queryKey: ["claims", "recent", { size: 8, sort: "createdAt,desc" }],
    queryFn: ({ signal }) => claimsService.list({ page: 0, size: 8, sort: "createdAt,desc" }, signal),
    staleTime: 15_000,
  });

  const reviewQueueQuery = useQuery({
    queryKey: ["claims", "review-queue"],
    queryFn: ({ signal }) =>
      claimsService.list({ status: "READY_FOR_REVIEW", page: 0, size: 5, sort: "createdAt,asc" }, signal),
    enabled: isOfficer,
    staleTime: 15_000,
  });

  const escalatedQuery = useQuery({
    queryKey: ["claims", "escalated"],
    queryFn: ({ signal }) =>
      claimsService.list({ status: "ESCALATED", page: 0, size: 5, sort: "createdAt,asc" }, signal),
    enabled: isOfficer,
    staleTime: 15_000,
  });

  const counts = useMemo(() => {
    const map = new Map<ClaimStatus, number>();
    STATUS_ORDER.forEach((s) => map.set(s, 0));
    (recentQuery.data?.content ?? []).forEach((c) => {
      map.set(c.status, (map.get(c.status) ?? 0) + 1);
    });
    return map;
  }, [recentQuery.data]);

  return (
    <>
      <PageHeader
        title={isClaimant ? "My claims" : "Dashboard"}
        description={
          isOfficer
            ? "Claims activity, review queue, and escalations."
            : "Your claims and their current status."
        }
        actions={
          hasRole(["CLAIMS_OFFICER"]) && (
            <Link to="/app/claims/new">
              <Button>
                <PlusCircle /> New claim
              </Button>
            </Link>
          )
        }
      />

      {isOfficer && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            icon={FileText}
            label="Ready for review"
            value={reviewQueueQuery.data?.totalElements}
            loading={reviewQueueQuery.isLoading}
            tone="warning"
          />
          <StatCard
            icon={AlertTriangle}
            label="Escalated"
            value={escalatedQuery.data?.totalElements}
            loading={escalatedQuery.isLoading}
            tone="destructive"
          />
          <StatCard
            icon={ClipboardCheck}
            label="Recently updated"
            value={recentQuery.data?.totalElements}
            loading={recentQuery.isLoading}
          />
          <StatCard
            icon={TrendingUp}
            label="Total visible"
            value={recentQuery.data?.totalElements}
            loading={recentQuery.isLoading}
          />
        </div>
      )}

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="text-base">Recent claims</CardTitle>
              <CardDescription>The latest claims visible to you.</CardDescription>
            </div>
            <Link to="/app/claims">
              <Button variant="ghost" size="sm">
                View all <ArrowRight />
              </Button>
            </Link>
          </CardHeader>
          <CardContent>
            {recentQuery.isLoading ? (
              <div className="space-y-2">
                {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : recentQuery.isError ? (
              <ErrorAlert error={recentQuery.error} onRetry={() => recentQuery.refetch()} />
            ) : (recentQuery.data?.content?.length ?? 0) === 0 ? (
              <EmptyState
                icon={FileText}
                title="No claims yet"
                description="Once claims are created they will appear here."
                action={
                  hasRole(["CLAIMS_OFFICER"]) ? (
                    <Link to="/app/claims/new">
                      <Button size="sm"><PlusCircle /> Create a claim</Button>
                    </Link>
                  ) : undefined
                }
              />
            ) : (
              <ul className="divide-y">
                {recentQuery.data!.content.map((c) => (
                  <RecentClaimRow key={c.id} claim={c} />
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        {isOfficer && (
          <div className="space-y-6">
            <QueueCard
              title="Review queue"
              description="Claims ready for officer review."
              icon={ClipboardCheck}
              query={reviewQueueQuery}
              emptyLabel="No claims awaiting review."
            />
            <QueueCard
              title="Escalations"
              description="Claims flagged for senior attention."
              icon={AlertTriangle}
              query={escalatedQuery}
              emptyLabel="No escalations right now."
            />
          </div>
        )}

        {isClaimant && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Status overview</CardTitle>
              <CardDescription>Counts across your recent claims.</CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2">
                {STATUS_ORDER.map((s) => (
                  <li key={s} className="flex items-center justify-between text-sm">
                    <ClaimStatusBadge status={s} />
                    <span className="font-mono text-xs text-muted-foreground">
                      {counts.get(s) ?? 0}
                    </span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        )}
      </div>
    </>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  loading,
  tone,
}: {
  icon: typeof FileText;
  label: string;
  value: number | undefined;
  loading: boolean;
  tone?: "warning" | "destructive";
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {label}
        </CardTitle>
        <Icon
          className={
            tone === "warning"
              ? "h-4 w-4 text-warning"
              : tone === "destructive"
              ? "h-4 w-4 text-destructive"
              : "h-4 w-4 text-muted-foreground"
          }
        />
      </CardHeader>
      <CardContent>
        {loading ? (
          <Skeleton className="h-8 w-12" />
        ) : (
          <p className="text-2xl font-semibold">{value ?? "—"}</p>
        )}
      </CardContent>
    </Card>
  );
}

function RecentClaimRow({ claim }: { claim: ClaimListItem }) {
  return (
    <li>
      <Link
        to={`/app/claims/${claim.id}`}
        className="flex items-center gap-4 py-3 transition-colors hover:bg-muted/40"
      >
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="font-mono text-xs text-muted-foreground">{claim.claimNumber}</span>
            {claim.riskFlag && (
              <Badge variant="destructive" className="text-[10px]">
                <AlertTriangle className="h-3 w-3" /> Risk
              </Badge>
            )}
          </div>
          <p className="mt-0.5 truncate text-sm font-medium">{claim.claimantName}</p>
          <p className="truncate text-xs text-muted-foreground">
            {claim.policyNumberStated} · Incident {claim.incidentDate}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <ClaimStatusBadge status={claim.status} />
          <span className="text-[10px] text-muted-foreground">
            {formatRelativeTime(claim.createdAt)}
          </span>
        </div>
      </Link>
    </li>
  );
}

function QueueCard({
  title,
  description,
  icon: Icon,
  query,
  emptyLabel,
}: {
  title: string;
  description: string;
  icon: typeof Activity;
  query: { isLoading: boolean; isError: boolean; error: unknown; data?: { content: ClaimListItem[] }; refetch: () => void };
  emptyLabel: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div>
          <CardTitle className="text-base">{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </div>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {query.isLoading ? (
          <div className="space-y-2">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
        ) : query.isError ? (
          <ErrorAlert error={query.error} onRetry={query.refetch} />
        ) : (query.data?.content?.length ?? 0) === 0 ? (
          <p className="text-xs text-muted-foreground">{emptyLabel}</p>
        ) : (
          <ul className="space-y-2">
            {query.data!.content.map((c) => (
              <li key={c.id}>
                <Link
                  to={`/app/claims/${c.id}`}
                  className="flex items-center justify-between rounded-md border bg-card p-2 text-xs transition-colors hover:bg-accent"
                >
                  <span className="min-w-0 flex-1 truncate">
                    <span className="font-mono">{c.claimNumber}</span>
                    <span className="ml-2 text-muted-foreground">{c.claimantName}</span>
                  </span>
                  <span className="ml-2 text-[10px] text-muted-foreground">
                    {formatRelativeTime(c.createdAt)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}