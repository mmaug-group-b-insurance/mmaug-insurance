import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import type { AuditEvent } from "@/types/audit";

export function AuditDetailsDrawer({
  event, open, onOpenChange,
}: {
  event: AuditEvent | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  if (!event) return null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Audit event</SheetTitle>
          <SheetDescription className="font-mono text-xs">{event.id}</SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-3 text-sm">
          <Row label="Occurred at" value={new Date(event.occurredAt).toLocaleString()} />
          <Row label="Actor" value={event.actor} />
          <Row label="Role" value={event.actorRole} />
          <Row label="Actor type" value={event.actorType} />
          <Row label="Action" value={<Badge variant="outline" className="font-mono text-[10px]">{event.action}</Badge>} />
          <Row label="Outcome" value={
            <Badge variant={event.outcome === "SUCCESS" ? "success" : event.outcome === "DENIED" ? "warning" : "destructive"} className="text-[10px]">
              {event.outcome}
            </Badge>
          } />
          <Row label="Entity" value={<span className="font-mono text-xs">{event.entityType} · {event.entityId}</span>} />
          {event.claimId && <Row label="Claim ID" value={<span className="font-mono text-xs">{event.claimId}</span>} />}
          {event.ipAddress && <Row label="IP address" value={<span className="font-mono text-xs">{event.ipAddress}</span>} />}
          {event.correlationId && <Row label="Correlation ID" value={<span className="font-mono text-xs">{event.correlationId}</span>} />}

          {event.details && Object.keys(event.details).length > 0 && (
            <div>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Details (redacted)</p>
              <pre className="mt-1 overflow-x-auto rounded-md border bg-muted/30 p-3 text-[10px]">
                {JSON.stringify(event.details, null, 2)}
              </pre>
            </div>
          )}

          <div className="rounded-md border bg-muted/20 p-3 text-[10px]">
            <p className="text-muted-foreground">Hash chain</p>
            <p className="mt-1 font-mono">prev {event.prevHash}</p>
            <p className="font-mono">hash {event.hash}</p>
          </div>

          <p className="text-[10px] italic text-muted-foreground">
            Audit events are insert-only. They cannot be edited or deleted from this application.
          </p>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[140px_1fr] gap-3">
      <span className="text-xs text-muted-foreground">{label}</span>
      <div>{value}</div>
    </div>
  );
}