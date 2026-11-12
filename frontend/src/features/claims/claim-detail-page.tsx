import { useEffect, useMemo } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle, ArrowLeft, Bot } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { claimsService } from "@/services/claims";
import { useAuth } from "@/features/auth/use-auth";
import { ClaimStatusBadge } from "./claim-status-badge";
import { CLAIM_TAB_KEYS, CLAIM_TAB_LABELS, type ClaimTabKey } from "./claim-tabs";
import { ClaimOverviewTab } from "./tabs/overview-tab";
import { ClaimDocumentsTab } from "./tabs/documents-tab";
import { ClaimFindingsTab } from "./tabs/findings-tab";
import { ClaimSummaryTab } from "./tabs/summary-tab";
import { ClaimReviewTab } from "./tabs/review-tab";
import { ClaimActivityTab } from "./tabs/activity-tab";

export function ClaimDetailPage() {
  const { id, tab } = useParams<{ id: string; tab?: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();

  const analyseMutation = useMutation({
    mutationFn: () => claimsService.analyse(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["claim", id] });
      queryClient.invalidateQueries({ queryKey: ["claim-summary", id] });
      queryClient.invalidateQueries({ queryKey: ["claim-summary-versions", id] });
      queryClient.invalidateQueries({ queryKey: ["claim-findings", id] });
      queryClient.invalidateQueries({ queryKey: ["claim-runs", id] });
      toast.success("Analysis complete", { description: "Grounded summary and findings generated." });
      navigate(`/app/claims/${id}/summary`);
    },
    onError: (err) => {
      toast.error("Analysis failed", { description: (err as Error).message });
    },
  });

  const activeTab: ClaimTabKey = useMemo(() => {
    if (!tab) return "overview";
    return (CLAIM_TAB_KEYS.includes(tab as ClaimTabKey) ? tab : "overview") as ClaimTabKey;
  }, [tab]);

  const query = useQuery({
    queryKey: ["claim", id],
    queryFn: ({ signal }) => claimsService.get(id!, signal),
    enabled: Boolean(id),
    staleTime: 15_000,
  });

  useEffect(() => {
    if (!tab && id) navigate(`/app/claims/${id}/overview`, { replace: true });
  }, [tab, id, navigate]);

  if (!id) return <Navigate to="/app/claims" replace />;

  const claim = query.data;

  if (query.isLoading) {
    return (
      <>
        <Skeleton className="h-6 w-32" />
        <Skeleton className="mt-4 h-28 w-full" />
        <Skeleton className="mt-4 h-10 w-80" />
        <Skeleton className="mt-4 h-64 w-full" />
      </>
    );
  }

  if (query.isError) {
    return (
      <>
        <Button variant="ghost" onClick={() => navigate("/app/claims")} className="mb-4">
          <ArrowLeft /> Back to claims
        </Button>
        <ErrorAlert error={query.error} onRetry={() => query.refetch()} title="Could not load claim" />
      </>
    );
  }

  if (!claim) return <Navigate to="/app/claims" replace />;

  if (!claim.id) {
    return (
      <>
        <Button variant="ghost" onClick={() => navigate("/app/claims")} className="mb-4">
          <ArrowLeft /> Back to claims
        </Button>
        <Alert variant="destructive">
          <AlertTriangle />
          <AlertTitle>Malformed claim response</AlertTitle>
          <AlertDescription>
            The backend returned a claim without an id. Check the browser network tab for the raw response.
          </AlertDescription>
        </Alert>
      </>
    );
  }

  const canReview = hasRole(["CLAIMS_OFFICER"]);
  const canAnalyse = hasRole(["CLAIMS_OFFICER"]);

  return (
    <>
      <Button variant="ghost" size="sm" onClick={() => navigate("/app/claims")} className="mb-4">
        <ArrowLeft /> All claims
      </Button>

      <PageHeader
        title={claim.claimNumber}
        description={claim.claimantName}
        actions={
          canAnalyse && (
            <div className="flex items-center gap-2">
              <Button
                onClick={() => analyseMutation.mutate()}
                disabled={analyseMutation.isPending}
              >
                <Bot className="h-4 w-4" />
                {analyseMutation.isPending ? "Analysing…" : "Run analysis"}
              </Button>
              <Button
                variant="outline"
                onClick={() => navigate(`/app/claims/${claim.id}/summary`)}
              >
                View summary
              </Button>
            </div>
          )
        }
      />

      <Card className="mb-4">
        <CardContent className="grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Status" value={<ClaimStatusBadge status={claim.status} />} />
          <Field
            label="Policy (stated)"
            value={<span className="font-mono text-xs">{claim.policyNumberStated}</span>}
          />
          <Field label="Vehicle" value={<span className="font-mono text-xs">{claim.vehicleRegistration}</span>} />
          <Field label="Incident date" value={<span className="text-sm">{claim.incidentDate}</span>} />
          {/* <Field label="Channel" value={<span className="text-sm">{claim.channel.replace(/_/g, " ")}</span>} /> */}
          <Field
  label="Channel"
  value={
    <span className="text-sm">
      {claim.channel ? claim.channel.replace(/_/g, " ") : "—"}
    </span>
  }
/>
          <Field
            label="Estimated amount"
            value={
              <span className="text-sm">
                {claim.estimatedAmount != null
                  ? new Intl.NumberFormat(undefined, { style: "currency", currency: claim.currency }).format(
                      claim.estimatedAmount
                    )
                  : "—"}
              </span>
            }
          />
          <Field
            label="Risk"
            value={
              claim.riskFlag ? (
                <Badge variant="destructive">
                  <AlertTriangle className="h-3 w-3" /> Flagged
                </Badge>
              ) : (
                <Badge variant="muted">Not flagged</Badge>
              )
            }
          />
          <Field label="Created" value={<span className="text-xs">{new Date(claim.createdAt).toLocaleString()}</span>} />
        </CardContent>
      </Card>

      {claim.escalationReason && (
        <Alert variant="warning" className="mb-4">
          <AlertTriangle />
          <AlertTitle>Escalation recommended</AlertTitle>
          <AlertDescription>{claim.escalationReason}</AlertDescription>
        </Alert>
      )}

      <Tabs
        value={activeTab}
        onValueChange={(v) => navigate(`/app/claims/${claim.id}/${v}`)}
        className="w-full"
      >
        <TabsList className="w-full justify-start overflow-x-auto">
          {CLAIM_TAB_KEYS.map((key) => (
            <TabsTrigger key={key} value={key} className="whitespace-nowrap">
              {CLAIM_TAB_LABELS[key]}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="overview"><ClaimOverviewTab claim={claim} /></TabsContent>
        <TabsContent value="documents"><ClaimDocumentsTab claim={claim} /></TabsContent>
        <TabsContent value="findings"><ClaimFindingsTab claim={claim} /></TabsContent>
        <TabsContent value="summary"><ClaimSummaryTab claim={claim} /></TabsContent>
        <TabsContent value="review">
          {canReview ? (
            <ClaimReviewTab claim={claim} />
          ) : (
            <Alert>
              <AlertTitle>Not available</AlertTitle>
              <AlertDescription>Review is only available to claims officers.</AlertDescription>
            </Alert>
          )}
        </TabsContent>
        <TabsContent value="activity"><ClaimActivityTab claim={claim} /></TabsContent>
      </Tabs>
    </>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <div className="mt-1">{value}</div>
    </div>
  );
}