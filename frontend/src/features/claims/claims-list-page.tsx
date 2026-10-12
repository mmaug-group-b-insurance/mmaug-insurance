import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import {
  AlertTriangle, ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, FileText,
  Filter, PlusCircle, Search, X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { ClaimStatusBadge } from "./claim-status-badge";
import { RiskFlagBadge } from "./risk-flag-badge";
import { claimsService } from "@/services/claims";
import { useAuth } from "@/features/auth/use-auth";
import { formatRelativeTime, truncate } from "@/lib/utils";
import type { ClaimStatus } from "@/types/claim";

const STATUSES: ClaimStatus[] = [
  "DRAFT", "SUBMITTED", "PROCESSING", "AWAITING_INFORMATION",
  "READY_FOR_REVIEW", "ESCALATED", "UNDER_INVESTIGATION", "CLOSED_BY_OFFICER",
];

type SortDir = "asc" | "desc";

export function ClaimsListPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();

  const page = Number(params.get("page") ?? "0");
  const size = 20;
  const status = (params.get("status") ?? "") as ClaimStatus | "";
  const claimNumber = params.get("claimNumber") ?? "";
  const policyNumber = params.get("policyNumber") ?? "";
  const claimantName = params.get("claimantName") ?? "";
  const riskFlag = params.get("riskFlag");
  const dateFrom = params.get("dateFrom") ?? "";
  const dateTo = params.get("dateTo") ?? "";
  const sortParam = params.get("sort") ?? "createdAt,desc";
  const [sortField, sortDir] = sortParam.split(",") as [string, SortDir];

  const [searchDraft, setSearchDraft] = useState(claimNumber);

  const filters = useMemo(
    () => ({
      page,
      size,
      sort: sortParam,
      status: status || undefined,
      claimNumber: claimNumber || undefined,
      policyNumber: policyNumber || undefined,
      claimantName: claimantName || undefined,
      riskFlag: riskFlag === "true" ? true : riskFlag === "false" ? false : undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
    }),
    [page, size, sortParam, status, claimNumber, policyNumber, claimantName, riskFlag, dateFrom, dateTo]
  );

  const query = useQuery({
    queryKey: ["claims", filters],
    queryFn: ({ signal }) => claimsService.list(filters, signal),
    placeholderData: keepPreviousData,
    staleTime: 15_000,
  });

  useEffect(() => {
    setSearchDraft(claimNumber);
  }, [claimNumber]);

  const updateParam = (key: string, value: string | null) => {
    const next = new URLSearchParams(params);
    if (value === null || value === "") next.delete(key);
    else next.set(key, value);
    if (key !== "page") next.delete("page");
    setParams(next, { replace: true });
  };

  const clearFilters = () => setParams(new URLSearchParams(), { replace: true });

  const toggleSort = (field: string) => {
    const nextDir: SortDir = sortField === field && sortDir === "desc" ? "asc" : "desc";
    updateParam("sort", `${field},${nextDir}`);
  };

  const hasFilters = Boolean(status || claimNumber || policyNumber || claimantName || riskFlag || dateFrom || dateTo);

  return (
    <>
      <PageHeader
        title="Claims"
        description={`${query.data?.totalElements ?? 0} claim${query.data?.totalElements === 1 ? "" : "s"} visible`}
        actions={
          hasRole(["CLAIMANT", "CLAIMS_OFFICER"]) && (
            <Link to="/app/claims/new">
              <Button><PlusCircle /> New claim</Button>
            </Link>
          )
        }
      />

      <Card className="mb-4">
        <CardContent className="p-4">
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={searchDraft}
                onChange={(e) => setSearchDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") updateParam("claimNumber", searchDraft);
                }}
                onBlur={() => updateParam("claimNumber", searchDraft)}
                placeholder="Claim number"
                className="pl-8"
              />
            </div>
            <Input
              value={policyNumber}
              onChange={(e) => updateParam("policyNumber", e.target.value)}
              placeholder="Policy number"
            />
            <Input
              value={claimantName}
              onChange={(e) => updateParam("claimantName", e.target.value)}
              placeholder="Claimant name"
            />
            <Select
              value={status || "ALL"}
              onValueChange={(v) => updateParam("status", v === "ALL" ? null : v)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All statuses</SelectItem>
                {STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>{s.replace(/_/g, " ").toLowerCase()}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input
              type="date"
              value={dateFrom}
              onChange={(e) => updateParam("dateFrom", e.target.value)}
              placeholder="From"
            />
            <Input
              type="date"
              value={dateTo}
              onChange={(e) => updateParam("dateTo", e.target.value)}
              placeholder="To"
            />
            <Select
              value={riskFlag ?? "ALL"}
              onValueChange={(v) => updateParam("riskFlag", v === "ALL" ? null : v)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Risk" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All</SelectItem>
                <SelectItem value="true">Risk flagged</SelectItem>
                <SelectItem value="false">Not flagged</SelectItem>
              </SelectContent>
            </Select>
            {hasFilters && (
              <Button variant="ghost" onClick={clearFilters} className="justify-start">
                <X /> Clear filters
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {query.isError ? (
        <ErrorAlert error={query.error} onRetry={() => query.refetch()} />
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <SortHeader field="claimNumber" label="Claim" sortField={sortField} sortDir={sortDir} onToggle={toggleSort} />
                  <TableHead>Claimant</TableHead>
                  <TableHead>Policy</TableHead>
                  <SortHeader field="incidentDate" label="Incident" sortField={sortField} sortDir={sortDir} onToggle={toggleSort} />
                  <TableHead>Status</TableHead>
                  <TableHead className="w-[80px]">Risk</TableHead>
                  <TableHead className="w-[90px] text-right">Docs</TableHead>
                  <TableHead className="w-[90px] text-right">Findings</TableHead>
                  <SortHeader field="createdAt" label="Created" sortField={sortField} sortDir={sortDir} onToggle={toggleSort} />
                </TableRow>
              </TableHeader>
              <TableBody>
                {query.isLoading
                  ? Array.from({ length: 6 }).map((_, i) => (
                      <TableRow key={i}>
                        <TableCell colSpan={9}><Skeleton className="h-6 w-full" /></TableCell>
                      </TableRow>
                    ))
                  : query.data?.content?.length === 0
                  ? (
                      <TableRow>
                        <TableCell colSpan={9} className="p-0">
                          <EmptyState
                            icon={Filter}
                            title={hasFilters ? "No claims match these filters" : "No claims yet"}
                            description={
                              hasFilters
                                ? "Adjust or clear the filters to see more results."
                                : "Created claims will appear here."
                            }
                            action={
                              hasFilters ? (
                                <Button variant="outline" onClick={clearFilters}>Clear filters</Button>
                              ) : hasRole(["CLAIMANT", "CLAIMS_OFFICER"]) ? (
                                <Link to="/app/claims/new">
                                  <Button><PlusCircle /> New claim</Button>
                                </Link>
                              ) : undefined
                            }
                          />
                        </TableCell>
                      </TableRow>
                    )
                  : query.data!.content.map((c) => (
                      <TableRow
                        key={c.id}
                        className="cursor-pointer"
                        onClick={() => navigate(`/app/claims/${c.id}`)}
                      >
                        <TableCell className="font-mono text-xs">{c.claimNumber}</TableCell>
                        <TableCell className="max-w-[180px] truncate">{truncate(c.claimantName, 28)}</TableCell>
                        <TableCell className="font-mono text-xs">{c.policyNumberStated}</TableCell>
                        <TableCell className="text-xs">{c.incidentDate}</TableCell>
                        <TableCell><ClaimStatusBadge status={c.status} /></TableCell>
                        <TableCell><RiskFlagBadge show={c.riskFlag} /></TableCell>
                        <TableCell className="text-right font-mono text-xs">{c.documentCount}</TableCell>
                        <TableCell className="text-right">
                          {c.openFindingCount > 0 ? (
                            <Badge variant="warning" className="font-mono text-[10px]">
                              <AlertTriangle className="h-3 w-3" /> {c.openFindingCount}
                            </Badge>
                          ) : (
                            <span className="text-xs text-muted-foreground">0</span>
                          )}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {formatRelativeTime(c.createdAt)}
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {query.data && query.data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <p className="text-xs text-muted-foreground">
            Page {query.data.page + 1} of {query.data.totalPages} · {query.data.totalElements} total
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 0}
              onClick={() => updateParam("page", String(page - 1))}
            >
              <ChevronLeft /> Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= (query.data.totalPages - 1)}
              onClick={() => updateParam("page", String(page + 1))}
            >
              Next <ChevronRight />
            </Button>
          </div>
        </div>
      )}
    </>
  );
}

function SortHeader({
  field, label, sortField, sortDir, onToggle,
}: {
  field: string;
  label: string;
  sortField: string;
  sortDir: SortDir;
  onToggle: (f: string) => void;
}) {
  const active = sortField === field;
  const Icon = !active ? ArrowUpDown : sortDir === "asc" ? ArrowUp : ArrowDown;
  return (
    <TableHead>
      <button
        type="button"
        onClick={() => onToggle(field)}
        className="inline-flex items-center gap-1 text-xs font-medium uppercase tracking-wide text-muted-foreground hover:text-foreground"
      >
        {label}
        <Icon className="h-3 w-3" />
      </button>
    </TableHead>
  );
}