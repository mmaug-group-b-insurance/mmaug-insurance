import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Bot, ClipboardCheck } from "lucide-react";
import { summariesService } from "@/services/summaries";
import { ReviewForm } from "@/features/review/review-form";
import { ReviewHistory } from "@/features/review/review-history";
import type { Claim } from "@/types/claim";

export function ClaimReviewTab({ claim }: { claim: Claim }) {
  const summaryQuery = useQuery({
    queryKey: ["claim-summary", claim.id, undefined],
    queryFn: ({ signal }) => summariesService.get(claim.id, undefined, signal),
    staleTime: 30_000,
    retry: 0,
  });

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ClipboardCheck className="h-4 w-4" /> Human review
          </CardTitle>
          <CardDescription>
            Record your review outcome. Approval, rejection, and payment are not available here.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {summaryQuery.isLoading ? (
            <Skeleton className="h-40 w-full" />
          ) : summaryQuery.isError ? (
            (summaryQuery.error as { status?: number })?.status === 404 ? (
              <EmptyState
                icon={Bot}
                title="No summary to review"
                description="Run the analysis pipeline before submitting a review."
              />
            ) : (
              <ErrorAlert error={summaryQuery.error} onRetry={() => summaryQuery.refetch()} />
            )
          ) : summaryQuery.data ? (
            <ReviewForm claimId={claim.id} summaryId={summaryQuery.data.summaryId} />
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Review history</CardTitle>
        </CardHeader>
        <CardContent>
          <ReviewHistory claimId={claim.id} />
        </CardContent>
      </Card>
    </div>
  );
}