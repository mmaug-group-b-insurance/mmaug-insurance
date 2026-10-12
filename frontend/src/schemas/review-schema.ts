import { z } from "zod";

export const reviewOutcomes = [
  "ACKNOWLEDGED",
  "INFO_REQUESTED",
  "ESCALATED_TO_SENIOR",
  "ACCEPTED_FOR_INVESTIGATION",
  "SUMMARY_REJECTED_REGENERATE",
] as const;

export const createReviewSchema = z
  .object({
    outcome: z.enum(reviewOutcomes),
    comment: z.string().min(1, "Comment is required").max(4000, "Max 4000 characters"),
    infoRequested: z.string().max(4000, "Max 4000 characters").optional().or(z.literal("")),
    agreedWithEscalation: z.boolean().optional(),
  })
  .refine(
    (data) => data.outcome !== "INFO_REQUESTED" || (data.infoRequested && data.infoRequested.trim().length > 0),
    { path: ["infoRequested"], message: "Describe what information is requested" }
  );

export type CreateReviewFormValues = z.infer<typeof createReviewSchema>;