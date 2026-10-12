import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { reviewsService } from "@/services/reviews";
import { ClipboardCheck } from "lucide-react";

export function ReviewHistory({ claimId }: { claimId: string }) {
  const query = useQuery({
    queryKey: ["claim-reviews", claimId],
    queryFn: ({ signal }) => reviewsService.list(claimId, signal),
    staleTime: 30_000,
  });

  if (query.isLoading) return <div className="space-y-2">{[0, 1].map((i) => <Skeleton key={i} className="h-20 w-full" />)}</div>;
  if (query.isError) return <ErrorAlert error={query.error} onRetry={() => query.refetch()} />;
  if (!query.data || query.data.length === 0) {
    return <EmptyState icon={ClipboardCheck} title="No reviews yet" description="Submitted reviews will appear here." />;
  }

  return (
    <ul className="space-y-3">
      {query.data.map((r) => (
        <li key={r.id}>
          <Card>
            <CardContent className="space-y-2 p-4 text-sm">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="outline" className="font-mono text-[10px]">{r.outcome}</Badge>
                <span className="text-xs text-muted-foreground">{r.reviewer}</span>
                <span className="text-[10px] text-muted-foreground">
                  {new Date(r.reviewedAt).toLocaleString()}
                </span>
              </div>
              <p className="whitespace-pre-wrap">{r.comment}</p>
              {r.infoRequested && (
                <div className="rounded-md border bg-muted/20 p-3">
                  <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Information requested</p>
                  <p className="mt-1 text-xs">{r.infoRequested}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </li>
      ))}
    </ul>
  );
}