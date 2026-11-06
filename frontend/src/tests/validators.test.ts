import { describe, it, expect } from "vitest";
import { createClaimSchema, e164Regex, policyNumberRegex } from "@/schemas/claim-schema";

describe("policyNumberRegex", () => {
  it("accepts POL-MT-###### form", () => {
    expect(policyNumberRegex.test("POL-MT-100001")).toBe(true);
    expect(policyNumberRegex.test("POL-MT-999999")).toBe(true);
  });
  it("rejects malformed values", () => {
    expect(policyNumberRegex.test("POL-MT-12345")).toBe(false);
    expect(policyNumberRegex.test("POLMT-100001")).toBe(false);
    expect(policyNumberRegex.test("pol-mt-100001")).toBe(false);
  });
});

describe("e164Regex", () => {
  it("accepts E.164 phone numbers", () => {
    expect(e164Regex.test("+35679123456")).toBe(true);
    expect(e164Regex.test("+14155552671")).toBe(true);
  });
  it("rejects non-E.164 values", () => {
    expect(e164Regex.test("79123456")).toBe(false);
    expect(e164Regex.test("(356) 7912 3456")).toBe(false);
    expect(e164Regex.test("+0")).toBe(false);
  });
});

describe("createClaimSchema", () => {
  const valid = {
    policyNumber: "POL-MT-100001",
    claimantName: "Jane Doe",
    claimantPhone: "+35679123456",
    claimantEmail: "jane@example.com",
    vehicleRegistration: "ABC 123",
    incidentDate: "2025-06-01",
    incidentLocation: "Valletta",
    incidentDescription: "Rear-ended at a roundabout on the way to work.",
    thirdPartyInvolved: false,
    injuriesReported: false,
  };

  it("accepts a valid payload", () => {
    const r = createClaimSchema.safeParse(valid);
    expect(r.success).toBe(true);
  });

  it.each([undefined, "", Number.NaN])("keeps an omitted or blank estimate unknown (%s)", (estimatedAmount) => {
    const result = createClaimSchema.parse({ ...valid, estimatedAmount });
    expect(result.estimatedAmount).toBeUndefined();
  });

  it("normalizes a decimal estimate from form text for the API", () => {
    expect(createClaimSchema.parse({ ...valid, estimatedAmount: "125.50" }).estimatedAmount).toBe(125.5);
  });

  it("rejects short description", () => {
    const r = createClaimSchema.safeParse({ ...valid, incidentDescription: "too short" });
    expect(r.success).toBe(false);
  });

  it("rejects a future incident date", () => {
    const future = new Date();
    future.setFullYear(future.getFullYear() + 1);
    const r = createClaimSchema.safeParse({ ...valid, incidentDate: future.toISOString().slice(0, 10) });
    expect(r.success).toBe(false);
  });

  it("rejects negative estimatedAmount", () => {
    const r = createClaimSchema.safeParse({ ...valid, estimatedAmount: -1 });
    expect(r.success).toBe(false);
  });

  it("rejects estimatedAmount with more than two decimals", () => {
    const r = createClaimSchema.safeParse({ ...valid, estimatedAmount: "100.123" });
    expect(r.success).toBe(false);
  });

  it("accepts estimatedAmount with two decimals", () => {
    const r = createClaimSchema.safeParse({ ...valid, estimatedAmount: "100.12" });
    expect(r.success).toBe(true);
  });
});
