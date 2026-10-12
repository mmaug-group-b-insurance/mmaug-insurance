import { describe, it, expect } from "vitest";
import { createReviewSchema } from "@/schemas/review-schema";

describe("createReviewSchema", () => {
  it("requires comment", () => {
    const r = createReviewSchema.safeParse({ outcome: "ACKNOWLEDGED", comment: "" });
    expect(r.success).toBe(false);
  });

  it("requires infoRequested when outcome is INFO_REQUESTED", () => {
    const r = createReviewSchema.safeParse({ outcome: "INFO_REQUESTED", comment: "ok" });
    expect(r.success).toBe(false);
  });

  it("accepts INFO_REQUESTED with details", () => {
    const r = createReviewSchema.safeParse({
      outcome: "INFO_REQUESTED",
      comment: "Requesting additional documentation.",
      infoRequested: "Please provide the police report number.",
    });
    expect(r.success).toBe(true);
  });

  it("rejects an unknown outcome", () => {
    const r = createReviewSchema.safeParse({ outcome: "APPROVED", comment: "no" });
    expect(r.success).toBe(false);
  });
});