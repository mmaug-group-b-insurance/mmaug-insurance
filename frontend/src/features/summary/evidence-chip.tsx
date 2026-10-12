import { useState } from "react";
import { FileText, Wrench, ScrollText, User } from "lucide-react";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { EvidenceDrawer } from "./evidence-drawer";
import type { EvidenceRef } from "@/types/finding";

const ICON: Record<EvidenceRef["sourceType"], typeof FileText> = {
  DOCUMENT: FileText,
  TOOL_RESULT: Wrench,
  RULE: ScrollText,
  CLAIM_FORM_FIELD: User,
};

export function EvidenceChip({
  evidence,
  variant = "outline",
}: {
  evidence: EvidenceRef;
  variant?: BadgeProps["variant"];
}) {
  const [open, setOpen] = useState(false);
  const Icon = ICON[evidence.sourceType] ?? FileText;

  const label =
    evidence.sourceType === "DOCUMENT"
      ? `${evidence.documentName ?? "document"}${evidence.page != null ? ` · p${evidence.page}` : ""}`
      : evidence.sourceType === "TOOL_RESULT"
      ? evidence.toolName ?? "tool result"
      : evidence.sourceType === "RULE"
      ? evidence.ruleId ?? "rule"
      : evidence.fieldName ?? "claim field";

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex"
        aria-label={`View evidence: ${label}`}
      >
        <Badge variant={variant} className="cursor-pointer gap-1 font-mono text-[10px] hover:bg-accent">
          <Icon className="h-3 w-3" />
          {label}
        </Badge>
      </button>
      <EvidenceDrawer evidence={evidence} open={open} onOpenChange={setOpen} />
    </>
  );
}