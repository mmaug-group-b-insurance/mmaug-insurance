import { FileText, Wrench, ScrollText, User } from "lucide-react";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import type { EvidenceRef } from "@/types/finding";

export function EvidenceDrawer({
  evidence, open, onOpenChange,
}: {
  evidence: EvidenceRef;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const Icon =
    evidence.sourceType === "DOCUMENT" ? FileText :
    evidence.sourceType === "TOOL_RESULT" ? Wrench :
    evidence.sourceType === "RULE" ? ScrollText : User;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-md">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <Icon className="h-4 w-4" /> Evidence
          </SheetTitle>
          <SheetDescription>{evidence.sourceType.replace(/_/g, " ").toLowerCase()}</SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-4 text-sm">
          {evidence.sourceType === "DOCUMENT" && (
            <div className="space-y-3">
              <Field label="Document" value={evidence.documentName ?? "—"} mono />
              <Field label="Page" value={evidence.page != null ? String(evidence.page) : "—"} />
              {evidence.snippet && (
                <div className="rounded-md border bg-muted/20 p-3">
                  <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Excerpt</p>
                  <p className="mt-1 whitespace-pre-wrap text-xs italic">“…{evidence.snippet}…”</p>
                </div>
              )}
            </div>
          )}

          {evidence.sourceType === "TOOL_RESULT" && (
            <div className="space-y-3">
              <Field label="Tool" value={evidence.toolName ?? "—"} mono />
              <Field label="Tool call" value={evidence.toolCallId ?? "—"} mono />
            </div>
          )}

          {evidence.sourceType === "RULE" && (
            <div className="space-y-3">
              <Field label="Rule" value={evidence.ruleId ?? "—"} mono />
            </div>
          )}

          {evidence.sourceType === "CLAIM_FORM_FIELD" && (
            <div className="space-y-3">
              <Field label="Claim field" value={evidence.fieldName ?? "—"} mono />
            </div>
          )}

          <div className="rounded-md border bg-muted/20 p-3 text-xs text-muted-foreground">
            <p>
              Evidence describes where the linked statement came from. It does not verify the correctness
              of the underlying source.
            </p>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <p className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <Badge variant="outline" className={mono ? "mt-1 font-mono text-xs" : "mt-1 text-xs"}>{value}</Badge>
    </div>
  );
}