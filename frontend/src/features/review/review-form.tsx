import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { reviewsService } from "@/services/reviews";
import { createReviewSchema, reviewOutcomes, type CreateReviewFormValues } from "@/schemas/review-schema";
import type { CreateReviewInput, ReviewOutcome } from "@/types/review";

const OUTCOME_LABEL: Record<ReviewOutcome, string> = {
  ACKNOWLEDGED: "Acknowledged",
  INFO_REQUESTED: "Information requested",
  ESCALATED_TO_SENIOR: "Escalated to senior",
  ACCEPTED_FOR_INVESTIGATION: "Accepted for investigation",
  SUMMARY_REJECTED_REGENERATE: "Summary rejected — regenerate",
};

export function ReviewForm({ claimId, summaryId }: { claimId: string; summaryId: string }) {
  const qc = useQueryClient();

  const form = useForm<CreateReviewFormValues>({
    resolver: zodResolver(createReviewSchema),
    defaultValues: {
      outcome: "ACKNOWLEDGED",
      comment: "",
      infoRequested: "",
      agreedWithEscalation: false,
    },
  });

  const mutation = useMutation({
    mutationFn: (values: CreateReviewFormValues) => {
      const payload: CreateReviewInput = {
        summaryId,
        outcome: values.outcome,
        comment: values.comment,
        infoRequested: values.outcome === "INFO_REQUESTED" ? values.infoRequested || undefined : undefined,
        agreedWithEscalation: values.agreedWithEscalation,
      };
      return reviewsService.create(claimId, payload);
    },
    onSuccess: () => {
      toast.success("Review submitted");
      qc.invalidateQueries({ queryKey: ["claim-reviews", claimId] });
      qc.invalidateQueries({ queryKey: ["claim", claimId] });
      form.reset();
    },
    onError: (err) => toast.error("Could not submit review", { description: (err as Error).message }),
  });

  const outcome = form.watch("outcome");

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
        className="space-y-4"
        noValidate
      >
        <Alert variant="info">
          <AlertTriangle />
          <AlertDescription>
            Accepting for investigation is <strong>not</strong> approval of payment. Approval and rejection
            are handled outside the agent and are not available here.
          </AlertDescription>
        </Alert>

        <FormField
          control={form.control}
          name="outcome"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Outcome</FormLabel>
              <Select value={field.value} onValueChange={field.onChange} disabled={mutation.isPending}>
                <FormControl>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                </FormControl>
                <SelectContent>
                  {reviewOutcomes.map((o) => (
                    <SelectItem key={o} value={o}>{OUTCOME_LABEL[o]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        {outcome === "INFO_REQUESTED" && (
          <FormField
            control={form.control}
            name="infoRequested"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Information requested</FormLabel>
                <FormControl>
                  <Textarea
                    {...field}
                    rows={3}
                    maxLength={4000}
                    placeholder="Describe the information required from the claimant."
                    disabled={mutation.isPending}
                  />
                </FormControl>
                <FormDescription>{(field.value ?? "").length}/4000</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        {outcome === "ESCALATED_TO_SENIOR" && (
          <FormField
            control={form.control}
            name="agreedWithEscalation"
            render={({ field }) => (
              <FormItem className="flex flex-row items-center gap-3 rounded-md border p-3">
                <FormControl>
                  <Checkbox
                    checked={field.value}
                    onCheckedChange={(v) => field.onChange(Boolean(v))}
                    disabled={mutation.isPending}
                  />
                </FormControl>
                <div className="space-y-0.5">
                  <FormLabel className="text-sm">I agree with the escalation recommendation</FormLabel>
                  <FormDescription>
                    Tick if you concur with the agent's escalation signal.
                  </FormDescription>
                </div>
              </FormItem>
            )}
          />
        )}

        <FormField
          control={form.control}
          name="comment"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Comment</FormLabel>
              <FormControl>
                <Textarea
                  {...field}
                  rows={5}
                  maxLength={4000}
                  placeholder="Explain your review outcome."
                  disabled={mutation.isPending}
                />
              </FormControl>
              <FormDescription>{field.value.length}/4000</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="flex justify-end">
          <Button type="submit" disabled={mutation.isPending}>
            <CheckCircle2 />
            {mutation.isPending ? "Submitting…" : "Submit review"}
          </Button>
        </div>
      </form>
    </Form>
  );
}