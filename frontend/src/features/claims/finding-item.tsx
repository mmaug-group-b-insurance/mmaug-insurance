import { useState } from "react";
import { AlertTriangle, CheckCircle2, ExternalLink, Link2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { SeverityBadge } from "./severity-badge";
import { EvidenceChip } from "@/features/summary/evidence-chip";
import { useAuth } from "@/features/auth/use-auth";
import type { Finding } from "@/types/finding";

interface FindingItemProps {
  finding: Finding;
  canResolve: boolean;
  onResolve: (note: string) => Promise<unknown>;
}

export function FindingItem({ finding, canResolve, onResolve }: FindingItemProps) {
  const [open, setOpen] = useState(false);
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const { hasRole } = useAuth();
  const isOfficer = hasRole(["CLAIMS_OFFICER"]);

  const handleResolve = async () => {
    if (!note.trim()) {
      toast.error("A resolution note is required.");
      return;
    }
    setBusy(true);
    try {
      await onResolve(note.trim());
      toast.success("Finding resolved");
      setOpen(false);
      setNote("");
    } catch (err) {
      toast.error("Could not resolve finding", { description: (err as Error).message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card className={finding.resolved ? "opacity-75" : undefined}>
      <CardContent className="space-y-3 p-4">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <SeverityBadge severity={finding.severity} />
            <Badge variant="outline" className="font-mono text-[10px]">{finding.type}</Badge>
            {finding.fieldName && <Badge variant="muted" className="font-mono text-[10px]">{finding.fieldName}</Badge>}
            <Badge variant="outline" className="text-[10px]">{finding.origin}</Badge>
            {finding.resolved && (
              <Badge variant="success" className="text-[10px]"><CheckCircle2 className="h-3 w-3" /> resolved</Badge>
            )}
          </div>
          {canResolve && (
            <Button size="sm" variant="outline" onClick={() => setOpen(true)}>
              Resolve
            </Button>
          )}
        </div>

        <p className="text-sm">{finding.message}</p>

        {finding.evidence.length > 0 && (
          <div>
            <p className="mb-1 flex items-center gap-1 text-[10px] uppercase tracking-wide text-muted-foreground">
              <Link2 className="h-3 w-3" /> Evidence
            </p>
            <div className="flex flex-wrap gap-1.5">
              {finding.evidence.map((e, i) => <EvidenceChip key={i} evidence={e} />)}
            </div>
          </div>
        )}

        {finding.resolved && finding.resolutionNote && (
          <div className="rounded-md border bg-muted/20 p-3 text-xs">
            <p className="text-muted-foreground">Resolution</p>
            <p className="mt-1">{finding.resolutionNote}</p>
            {finding.resolvedAt && (
              <p className="mt-1 text-[10px] text-muted-foreground">
                Resolved {new Date(finding.resolvedAt).toLocaleString()}
              </p>
            )}
          </div>
        )}

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-warning" /> Resolve finding
              </DialogTitle>
              <DialogDescription>
                Record how this finding was addressed. This becomes part of the audit trail.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-2">
              <Label htmlFor="resolution-note">Resolution note</Label>
              <Textarea
                id="resolution-note"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={4}
                maxLength={500}
                placeholder="Describe the resolution and any relevant evidence."
              />
              <p className="text-xs text-muted-foreground">{note.length}/500</p>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setOpen(false)} disabled={busy}>Cancel</Button>
              <Button onClick={handleResolve} disabled={busy}>{busy ? "Saving…" : "Mark resolved"}</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {!isOfficer && !finding.resolved && (
          <p className="flex items-center gap-1 text-[10px] text-muted-foreground">
            <ExternalLink className="h-3 w-3" /> Only claims officers can resolve findings.
          </p>
        )}
      </CardContent>
    </Card>
  );
}