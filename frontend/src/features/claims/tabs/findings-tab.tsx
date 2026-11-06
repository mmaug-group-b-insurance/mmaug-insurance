import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Filter } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { findingsService } from "@/services/findings";
import { useAuth } from "@/features/auth/use-auth";
import { FindingItem } from "../finding-item";
import type { Claim } from "@/types/claim";
import type { FindingType, Severity } from "@/types/finding";

const SEVERITIES: Severity[] = ["INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL"];
const TYPES: FindingType[] = [
  "MISSING_FIELD", "CONTRADICTION", "LOW_CONFIDENCE", "POLICY_NOT_FOUND",
  "POLICY_INACTIVE_ON_DATE", "COVERAGE_MISMATCH", "VEHICLE_MISMATCH", "DRIVER_NOT_LISTED",
  "LATE_REPORTING", "HIGH_VALUE", "FREQUENT_CLAIMANT", "INJECTION_SUSPECTED",
  "TOOL_UNAVAILABLE", "OTHER",
];

export function ClaimFindingsTab({ claim }: { claim: Claim }) {
  const qc = useQueryClient();
  const { hasRole } = useAuth();
  const canResolve = hasRole(["CLAIMS_OFFICER"]);
  const [severity, setSeverity] = useState<string>("ALL");
  const [type, setType] = useState<string>("ALL");
  const [resolved, setResolved] = useState<string>("false");

  const query = useQuery({
    queryKey: ["claim-findings", claim.id, { severity, type, resolved }],
    queryFn: ({ signal }) =>
      findingsService.list(
        claim.id,
        {
          severity: severity === "ALL" ? undefined : severity,
          type: type === "ALL" ? undefined : type,
          resolved: resolved === "ALL" ? undefined : resolved === "true",
        },
        signal
      ),
    staleTime: 15_000,
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) => findingsService.resolve(id, note),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["claim-findings", claim.id] });
    },
  });

  const hasFilters = severity !== "ALL" || type !== "ALL" || resolved !== "false";

  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="grid gap-3 p-4 sm:grid-cols-3">
          <Select value={severity} onValueChange={setSeverity}>
            <SelectTrigger><SelectValue placeholder="Severity" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All severities</SelectItem>
              {SEVERITIES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={type} onValueChange={setType}>
            <SelectTrigger><SelectValue placeholder="Type" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All types</SelectItem>
              {TYPES.map((t) => <SelectItem key={t} value={t}>{t.replace(/_/g, " ").toLowerCase()}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={resolved} onValueChange={setResolved}>
            <SelectTrigger><SelectValue placeholder="Resolution" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="false">Unresolved</SelectItem>
              <SelectItem value="true">Resolved</SelectItem>
              <SelectItem value="ALL">All</SelectItem>
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {query.isLoading ? (
        <div className="space-y-2">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-24 w-full" />)}</div>
      ) : query.isError ? (
        <ErrorAlert error={query.error} onRetry={() => query.refetch()} />
      ) : (query.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={hasFilters ? Filter : CheckCircle2}
          title={hasFilters ? "No findings match the filters" : "No open findings"}
          description={hasFilters ? "Adjust the filters to see more." : "The validation layer found no issues for this claim."}
        />
      ) : (
        <ul className="space-y-3">
          {query.data!.map((f) => (
            <li key={f.id}>
              <FindingItem
                finding={f}
                canResolve={canResolve && !f.resolved}
                onResolve={(note) => resolveMutation.mutateAsync({ id: f.id, note })}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}