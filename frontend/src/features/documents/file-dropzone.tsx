import { useCallback, useId, useRef, useState } from "react";
import { AlertCircle, FileText, Image as ImageIcon, Trash2, UploadCloud } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import type { DocumentType } from "@/types/document";

export const DOCUMENT_TYPES: { value: DocumentType; label: string }[] = [
  { value: "CLAIM_FORM", label: "Claim form" },
  { value: "POLICE_REPORT", label: "Police report" },
  { value: "REPAIR_ESTIMATE", label: "Repair estimate" },
  { value: "PHOTO", label: "Photo" },
  { value: "INCIDENT_REPORT", label: "Incident report" },
  { value: "MEDICAL_REPORT", label: "Medical report" },
  { value: "WITNESS_STATEMENT", label: "Witness statement" },
  { value: "OTHER", label: "Other" },
];

export const MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;
export const MAX_FILES = 20;
export const ALLOWED_MIME = [
  "application/pdf",
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/tiff",
];
const ALLOWED_EXT = /\.(pdf|jpe?g|png|tiff?)$/i;

export interface PendingFile {
  file: File;
  documentType: DocumentType;
}

interface FileDropzoneProps {
  disabled?: boolean;
  maxFiles?: number;
  initialItems?: PendingFile[];
  onFilesChange?: (items: PendingFile[]) => void;
  onUploadAll?: (items: PendingFile[]) => Promise<void>;
  uploadLabel?: string;
  className?: string;
}

export function FileDropzone({
  disabled,
  maxFiles = MAX_FILES,
  initialItems = [],
  onFilesChange,
  onUploadAll,
  uploadLabel = "Upload all",
  className,
}: FileDropzoneProps) {
  const inputId = useId();
  const [items, setItems] = useState<PendingFile[]>(initialItems);
  const [drag, setDrag] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const updateItems = useCallback(
    (next: PendingFile[]) => {
      setItems(next);
      onFilesChange?.(next);
    },
    [onFilesChange]
  );

  const validateAndAdd = useCallback(
    (files: FileList | File[]) => {
      const incoming = Array.from(files);
      const nextErrors: string[] = [];
      const accepted: PendingFile[] = [];

      for (const f of incoming) {
        if (!ALLOWED_MIME.includes(f.type) && !ALLOWED_EXT.test(f.name)) {
          nextErrors.push(`${f.name}: unsupported type (PDF, JPG, JPEG, PNG, TIFF only).`);
          continue;
        }
        if (f.size > MAX_FILE_SIZE_BYTES) {
          nextErrors.push(`${f.name}: exceeds 15 MB.`);
          continue;
        }
        if (items.length + accepted.length >= maxFiles) {
          nextErrors.push(`Limit of ${maxFiles} files reached.`);
          break;
        }
        accepted.push({ file: f, documentType: guessDocumentType(f.name) });
      }

      setErrors(nextErrors);
      if (accepted.length > 0) {
        updateItems([...items, ...accepted]);
      }
    },
    [items, maxFiles, updateItems]
  );

  const onDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDrag(false);
    if (disabled) return;
    if (e.dataTransfer.files?.length) validateAndAdd(e.dataTransfer.files);
  };

  const removeItem = (index: number) => {
    const next = items.slice();
    next.splice(index, 1);
    updateItems(next);
  };

  const setType = (index: number, t: DocumentType) => {
    const next = items.slice();
    next[index] = { ...next[index], documentType: t };
    updateItems(next);
  };

  const handleUploadAll = async () => {
    if (!onUploadAll || items.length === 0) return;
    setUploading(true);
    try {
      await onUploadAll(items);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className={cn("space-y-4", className)}>
      <div
        onDragOver={(e) => { e.preventDefault(); if (!disabled) setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={onDrop}
        className={cn(
          "relative flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-8 text-center transition-colors",
          drag ? "border-primary bg-primary/5" : "border-muted-foreground/25",
          disabled && "cursor-not-allowed opacity-60"
        )}
      >
        <UploadCloud className="h-6 w-6 text-muted-foreground" />
        <p className="mt-2 text-sm font-medium">Drag files here or</p>
        <label htmlFor={inputId} className="mt-1 cursor-pointer text-sm text-primary underline-offset-4 hover:underline">
          browse your device
        </label>
        <input
          id={inputId}
          ref={inputRef}
          type="file"
          accept=".pdf,.jpg,.jpeg,.png,.tif,.tiff,application/pdf,image/jpeg,image/png,image/tiff"
          multiple
          className="sr-only"
          disabled={disabled}
          onChange={(e) => {
            if (e.target.files) validateAndAdd(e.target.files);
            e.target.value = "";
          }}
        />
        <p className="mt-3 text-xs text-muted-foreground">
          PDF, JPG, JPEG, PNG, TIFF · up to 15 MB each · up to {maxFiles} files
        </p>
      </div>

      {errors.length > 0 && (
        <ul className="space-y-1">
          {errors.map((e, i) => (
            <li key={i} className="flex items-center gap-2 text-xs text-destructive">
              <AlertCircle className="h-3.5 w-3.5" /> {e}
            </li>
          ))}
        </ul>
      )}

      {items.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            {items.length} file{items.length === 1 ? "" : "s"} ready
          </p>
          <ul className="space-y-2">
            {items.map((item, i) => (
              <li
                key={`${item.file.name}-${i}`}
                className="flex flex-col gap-3 rounded-md border bg-card p-3 sm:flex-row sm:items-center"
              >
                <div className="flex min-w-0 flex-1 items-center gap-3">
                  {item.file.type.startsWith("image/") ? (
                    <ImageIcon className="h-4 w-4 shrink-0 text-muted-foreground" />
                  ) : (
                    <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                  )}
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{item.file.name}</p>
                    <p className="text-xs text-muted-foreground">{formatBytes(item.file.size)}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Select
                    value={item.documentType}
                    onValueChange={(v) => setType(i, v as DocumentType)}
                    disabled={disabled || uploading}
                  >
                    <SelectTrigger className="h-8 w-[160px] text-xs">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DOCUMENT_TYPES.map((t) => (
                        <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={`Remove ${item.file.name}`}
                    onClick={() => removeItem(i)}
                    disabled={disabled || uploading}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>

          {onUploadAll && (
            <div className="flex items-center justify-between pt-1">
              <Badge variant="muted" className="text-[10px]">
                Server re-validates every file.
              </Badge>
              <Button onClick={handleUploadAll} disabled={uploading || disabled}>
                {uploading ? "Uploading…" : uploadLabel}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function guessDocumentType(filename: string): DocumentType {
  const n = filename.toLowerCase();
  if (n.includes("police")) return "POLICE_REPORT";
  if (n.includes("estimate") || n.includes("quote")) return "REPAIR_ESTIMATE";
  if (n.includes("photo") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) return "PHOTO";
  if (n.includes("form")) return "CLAIM_FORM";
  if (n.includes("medical")) return "MEDICAL_REPORT";
  if (n.includes("witness")) return "WITNESS_STATEMENT";
  return "OTHER";
}