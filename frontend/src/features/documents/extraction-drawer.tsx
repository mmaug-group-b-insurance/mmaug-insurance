import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, FileText } from "lucide-react";
import {
  Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle,
} from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { extractionService } from "@/services/extraction";
import { formatRelativeTime } from "@/lib/utils";
import type { ClaimDocument } from "@/types/document";

export function ExtractionDrawer({
  document, open, onOpenChange,
}: {
  document: ClaimDocument | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const query = useQuery({
    queryKey: ["extraction", document?.id],
    queryFn: ({ signal }) => extractionService.get(document!.id, signal),
    enabled: Boolean(document) && open,
    staleTime: 60_000,
  });

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full overflow-y-auto sm:max-w-xl">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <FileText className="h-4 w-4" /> Extraction
          </SheetTitle>
          <SheetDescription className="truncate">{document?.originalFilename}</SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-4">
          {query.isLoading ? (
            <div className="space-y-2">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : query.isError ? (
            <ErrorAlert error={query.error} onRetry={() => query.refetch()} />
          ) : !query.data ? (
            <EmptyState icon={FileText} title="No extraction yet" description="This document has not been processed." />
          ) : (
            <>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <Meta label="Status" value={<Badge variant="muted">{query.data.status}</Badge>} />
                <Meta label="Overall confidence" value={query.data.overallConfidence != null ? query.data.overallConfidence.toFixed(2) : "—"} />
                <Meta label="Extractor" value={query.data.extractorModel} />
                <Meta label="LLM" value={query.data.llmModel ?? "—"} />
                <Meta label="Prompt version" value={query.data.promptVersion} />
                <Meta label="Schema version" value={query.data.schemaVersion} />
              </div>

              {query.data.injectionSuspected && (
                <Alert variant="destructive">
                  <AlertTriangle />
                  <AlertTitle>Prompt-injection suspected</AlertTitle>
                  <AlertDescription>
                    Content in this document attempted to issue instructions. It was ignored and flagged.
                  </AlertDescription>
                </Alert>
              )}

              <div>
                <h3 className="mb-2 text-sm font-semibold">Extracted fields</h3>
                {query.data.fields.length === 0 ? (
                  <p className="text-xs text-muted-foreground">No fields extracted.</p>
                ) : (
                  <ul className="space-y-2">
                    {query.data.fields.map((f, i) => (
                      <li key={i} className="rounded-md border bg-card p-3 text-sm">
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-xs text-muted-foreground">{f.fieldName}</span>
                          <ConfidenceChip value={f.confidence} />
                        </div>
                        <p className="mt-1 break-words">{f.value}</p>
                        {f.normalisedValue && f.normalisedValue !== f.value && (
                          <p className="mt-1 text-xs text-muted-foreground">
                            Normalised: <span className="font-mono">{f.normalisedValue}</span>
                          </p>
                        )}
                        {f.snippet && (
                          <p className="mt-2 border-l-2 border-muted pl-2 text-xs italic text-muted-foreground">
                            “…{f.snippet}…”
                          </p>
                        )}
                        <div className="mt-2 flex items-center gap-2 text-[10px] text-muted-foreground">
                          {f.page != null && <Badge variant="outline" className="text-[10px]">page {f.page}</Badge>}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <p className="text-[10px] text-muted-foreground">
                Extractions are not verified facts. Officers verify against the source document before relying on them.
              </p>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}

function ConfidenceChip({ value }: { value: number }) {
  const tone = value >= 0.85 ? "success" : value >= 0.6 ? "warning" : "destructive";
  return <Badge variant={tone}>{value.toFixed(2)}</Badge>;
}

function Meta({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <div className="mt-0.5">{value}</div>
    </div>
  );
}

export { formatRelativeTime };