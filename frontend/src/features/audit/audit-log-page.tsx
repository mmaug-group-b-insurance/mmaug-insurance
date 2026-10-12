import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, ScrollText, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { auditService } from "@/services/audit";
import { AuditDetailsDrawer } from "./audit-details-drawer";
import { formatRelativeTime } from "@/lib/utils";
import type { AuditEvent } from "@/types/audit";

export function AuditLogPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    claimId: "",
    actorUserId: "",
    action: "",
    outcome: "",
    from: "",
    to: "",
    correlationId: "",
  });
  const [selected, setSelected] = useState<AuditEvent | null>(null);

  const query = useQuery({
    queryKey: ["audit-events", { ...filters, page }],
    queryFn: ({ signal }) =>
      auditService.list(
        {
          claimId: filters.claimId || undefined,
          actorUserId: filters.actorUserId || undefined,
          action: filters.action || undefined,
          outcome: filters.outcome || undefined,
          from: filters.from || undefined,
          to: filters.to || undefined,
          correlationId: filters.correlationId || undefined,
          page,
          size: 30,
        },
        signal
      ),
    placeholderData: keepPreviousData,
    staleTime: 15_000,
  });

  const update = (key: keyof typeof filters, value: string) => {
    setFilters((f) => ({ ...f, [key]: value }));
    setPage(0);
  };

  const clear = () => {
    setFilters({ claimId: "", actorUserId: "", action: "", outcome: "", from: "", to: "", correlationId: "" });
    setPage(0);
  };

  const hasFilters = Object.values(filters).some(Boolean);

  return (
    <>
      <PageHeader title="Audit log" description="Tamper-evident event history. This view is read-only." />

      <Card className="mb-4">
        <CardContent className="grid gap-3 p-4 md:grid-cols-3 lg:grid-cols-4">
          <Input placeholder="Claim ID" value={filters.claimId} onChange={(e) => update("claimId", e.target.value)} />
          <Input placeholder="Actor user ID" value={filters.actorUserId} onChange={(e) => update("actorUserId", e.target.value)} />
          <Input placeholder="Action" value={filters.action} onChange={(e) => update("action", e.target.value)} />
          <Input placeholder="Outcome" value={filters.outcome} onChange={(e) => update("outcome", e.target.value)} />
          <Input type="datetime-local" value={filters.from} onChange={(e) => update("from", e.target.value)} />
          <Input type="datetime-local" value={filters.to} onChange={(e) => update("to", e.target.value)} />
          <Input placeholder="Correlation ID" value={filters.correlationId} onChange={(e) => update("correlationId", e.target.value)} />
          {hasFilters && (
            <Button variant="ghost" onClick={clear} className="justify-start">
              <X /> Clear filters
            </Button>
          )}
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
                  <TableHead>Time</TableHead>
                  <TableHead>Actor</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Outcome</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>Correlation</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {query.isLoading
                  ? Array.from({ length: 8 }).map((_, i) => (
                      <TableRow key={i}><TableCell colSpan={7}><Skeleton className="h-6 w-full" /></TableCell></TableRow>
                    ))
                  : query.data?.content?.length === 0
                  ? (
                      <TableRow>
                        <TableCell colSpan={7} className="p-0">
                          <EmptyState
                            icon={ScrollText}
                            title={hasFilters ? "No events match the filters" : "No audit events"}
                            description={hasFilters ? "Adjust or clear filters." : "Events will appear here as they occur."}
                          />
                        </TableCell>
                      </TableRow>
                    )
                  : query.data!.content.map((e) => (
                      <TableRow
                        key={e.id}
                        className="cursor-pointer"
                        onClick={() => setSelected(e)}
                      >
                        <TableCell className="text-xs text-muted-foreground">{formatRelativeTime(e.occurredAt)}</TableCell>
                        <TableCell className="text-xs">{e.actor}</TableCell>
                        <TableCell><Badge variant="muted" className="text-[10px]">{e.actorRole}</Badge></TableCell>
                        <TableCell className="font-mono text-xs">{e.action}</TableCell>
                        <TableCell>
                          <Badge
                            variant={e.outcome === "SUCCESS" ? "success" : e.outcome === "DENIED" ? "warning" : "destructive"}
                            className="text-[10px]"
                          >
                            {e.outcome}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs">
                          <span className="text-muted-foreground">{e.entityType}</span>{" "}
                          <span className="font-mono">{e.entityId.slice(0, 8)}</span>
                        </TableCell>
                        <TableCell className="font-mono text-[10px] text-muted-foreground">
                          {e.correlationId?.slice(0, 8) ?? "—"}
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
            Page {query.data.page + 1} of {query.data.totalPages} · {query.data.totalElements} events
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
              <ChevronLeft /> Previous
            </Button>
            <Button variant="outline" size="sm" disabled={page >= query.data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>
              Next <ChevronRight />
            </Button>
          </div>
        </div>
      )}

      <AuditDetailsDrawer event={selected} open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)} />
    </>
  );
}