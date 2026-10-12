import { format, formatDistanceToNow } from "date-fns";
import { cn } from "@/lib/utils";

export function DateTime({ value, className, mode = "absolute" }: { value: string | Date | null | undefined; className?: string; mode?: "absolute" | "relative" }) {
  if (!value) return <span className={cn("text-muted-foreground", className)}>—</span>;
  const d = typeof value === "string" ? new Date(value) : value;
  const text = mode === "relative" ? formatDistanceToNow(d, { addSuffix: true }) : format(d, "d MMM yyyy, HH:mm");
  return <time dateTime={d.toISOString()} className={className} title={d.toISOString()}>{text}</time>;
}