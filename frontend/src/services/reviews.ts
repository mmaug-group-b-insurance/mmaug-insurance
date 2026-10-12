import { apiRequest } from "@/lib/api-client";
import type { CreateReviewInput, Review } from "@/types/review";

export const reviewsService = {
  list(claimId: string, signal?: AbortSignal) {
    return apiRequest<Review[]>(`/claims/${claimId}/reviews`, { signal });
  },
  create(claimId: string, input: CreateReviewInput) {
    return apiRequest<Review>(`/claims/${claimId}/reviews`, { method: "POST", body: input });
  },
};