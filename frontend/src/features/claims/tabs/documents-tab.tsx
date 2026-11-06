import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Download, FileText, Image as ImageIcon, RefreshCcw, Search, Trash2, Upload,
} from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FileDropzone, type PendingFile } from "@/features/documents/file-dropzone";
import { ExtractionDrawer } from "@/features/documents/extraction-drawer";
import { documentsService } from "@/services/documents";
import { useAuth } from "@/features/auth/use-auth";
import { formatRelativeTime } from "@/lib/utils";
import { ApiRequestError } from "@/lib/api-client";
import type { Claim } from "@/types/claim";
import type { ClaimDocument } from "@/types/document";

const PROCESSING_LABEL: Record<ClaimDocument["processingStatus"], { label: string; tone: "muted" | "info" | "success" | "warning" | "destructive" }> = {
  UPLOADED: { label: "Uploaded", tone: "info" },
  QUARANTINED: { label: "Quarantined", tone: "warning" },
  EXTRACTING: { label: "Extracting", tone: "info" },
  EXTRACTED: { label: "Extracted", tone: "success" },
  FAILED: { label: "Failed", tone: "destructive" },
};

const SCAN_LABEL: Record<ClaimDocument["scanStatus"], { label: string; tone: "muted" | "success" | "warning" | "destructive" }> = {
  PENDING: { label: "Scan pending", tone: "muted" },
  CLEAN: { label: "Clean", tone: "success" },
  INFECTED: { label: "Infected", tone: "destructive" },
  REJECTED: { label: "Rejected", tone: "destructive" },
};

export function ClaimDocumentsTab({ claim }: { claim: Claim }) {
  const qc = useQueryClient();
  const { hasRole } = useAuth();
  const canUpload = hasRole(["CLAIMANT", "CLAIMS_OFFICER"]);
  const canDelete = hasRole(["ADMIN"]);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ClaimDocument | null>(null);
  const [extractionTarget, setExtractionTarget] = useState<ClaimDocument | null>(null);
  const [, setPending] = useState<PendingFile[]>([]);
  const dropzoneRef = useRef<HTMLDivElement>(null);

  const docsQuery = useQuery({
    queryKey: ["claim-documents", claim.id],
    queryFn: ({ signal }) => documentsService.listForClaim(claim.id, signal),
    staleTime: 10_000,
  });

  const uploadMutation = useMutation({
    mutationFn: async (items: PendingFile[]) => {
      const results: ClaimDocument[] = [];
      for (const item of items) {
        const doc = await documentsService.upload(claim.id, item.file, item.documentType);
        results.push(doc);
      }
      return results;
    },
    onSuccess: (results) => {
      qc.invalidateQueries({ queryKey: ["claim-documents", claim.id] });
      toast.success(`${results.length} file${results.length === 1 ? "" : "s"} uploaded`, {
        description: "Scan and extraction will begin automatically.",
      });
      setPending([]);
      setUploadOpen(false);
    },
    onError: (err) => {
      const msg = err instanceof ApiRequestError ? err.message : "Upload failed";
      toast.error("Upload failed", { description: msg });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (documentId: string) => documentsService.remove(documentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["claim-documents", claim.id] });
      toast.success("Document deleted");
      setDeleteTarget(null);
    },
    onError: (err) => toast.error("Could not delete document", { description: (err as Error).message }),
  });

  return (
    <>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle className="text-base">Documents</CardTitle>
            <CardDescription>Uploaded files and their processing status.</CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={() => docsQuery.refetch()} aria-label="Refresh">
              <RefreshCcw />
            </Button>
            {canUpload && (
              <Button size="sm" onClick={() => setUploadOpen((v) => !v)}>
                <Upload /> {uploadOpen ? "Close" : "Upload"}
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent ref={dropzoneRef}>
          {uploadOpen && canUpload && (
            <div className="mb-4 rounded-md border bg-muted/20 p-4">
              <FileDropzone
                disabled={uploadMutation.isPending}
                onFilesChange={setPending}
                onUploadAll={(items) => uploadMutation.mutateAsync(items).then(() => undefined)}
                uploadLabel={uploadMutation.isPending ? "Uploading…" : "Upload all"}
              />
            </div>
          )}

          {docsQuery.isLoading ? (
            <div className="space-y-2">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : docsQuery.isError ? (
            <ErrorAlert error={docsQuery.error} onRetry={() => docsQuery.refetch()} />
          ) : (docsQuery.data?.length ?? 0) === 0 ? (
            <EmptyState
              icon={FileText}
              title="No documents yet"
              description="Uploaded documents will appear here with their scan and extraction status."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>File</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Scan</TableHead>
                  <TableHead>Processing</TableHead>
                  <TableHead className="text-right">Pages</TableHead>
                  <TableHead>Uploaded</TableHead>
                  <TableHead className="w-[140px] text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {docsQuery.data!.map((d) => {
                  const proc = PROCESSING_LABEL[d.processingStatus];
                  const scan = SCAN_LABEL[d.scanStatus];
                  return (
                    <TableRow key={d.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {d.contentType.startsWith("image/") ? (
                            <ImageIcon className="h-4 w-4 text-muted-foreground" />
                          ) : (
                            <FileText className="h-4 w-4 text-muted-foreground" />
                          )}
                          <span className="max-w-[220px] truncate text-sm">{d.originalFilename}</span>
                        </div>
                        <p className="mt-0.5 font-mono text-[10px] text-muted-foreground">
                          {d.sha256.slice(0, 12)}… · {formatBytes(d.sizeBytes)}
                        </p>
                      </TableCell>
                      <TableCell className="text-xs">
                        {d.documentType.replace(/_/g, " ")}
                        {d.detectedType && d.detectedType !== d.documentType && (
                          <Badge variant="warning" className="ml-2 text-[10px]">detected: {d.detectedType}</Badge>
                        )}
                      </TableCell>
                      <TableCell><Badge variant={scan.tone}>{scan.label}</Badge></TableCell>
                      <TableCell><Badge variant={proc.tone}>{proc.label}</Badge></TableCell>
                      <TableCell className="text-right font-mono text-xs">{d.pageCount ?? "—"}</TableCell>
                      <TableCell className="text-xs text-muted-foreground">{formatRelativeTime(d.uploadedAt)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" size="icon" aria-label="Extraction" onClick={() => setExtractionTarget(d)}>
                            <Search className="h-4 w-4" />
                          </Button>
                          <a
                            href={documentsService.downloadUrl(d.id)}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
                            aria-label="Download"
                          >
                            <Download className="h-4 w-4" />
                          </a>
                          {canDelete && (
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label="Delete"
                              onClick={() => setDeleteTarget(d)}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Delete this document?"
        description={deleteTarget?.originalFilename}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        destructive
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id);
        }}
      />

      <ExtractionDrawer
        document={extractionTarget}
        open={Boolean(extractionTarget)}
        onOpenChange={(open) => !open && setExtractionTarget(null)}
      />
    </>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}
