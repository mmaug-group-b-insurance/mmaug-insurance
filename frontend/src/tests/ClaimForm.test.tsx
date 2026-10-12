import { describe, it, expect, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "./test-utils";
import { NewClaimPage } from "@/features/claims/new-claim-page";

vi.mock("@/services/claims", () => ({
  claimsService: {
    create: vi.fn().mockResolvedValue({
      id: "claim-1",
      claimNumber: "CLM-2026-000001",
      status: "DRAFT",
      channel: "WEB_PORTAL",
      policyNumberStated: "POL-MT-100001",
      claimantName: "Jane Doe",
      vehicleRegistration: "ABC 123",
      incidentDate: "2025-06-01",
      incidentLocation: "Valletta",
      incidentDescription: "Rear-ended at a roundabout on the way to work.",
      thirdPartyInvolved: false,
      injuriesReported: false,
      currency: "EUR",
      riskFlag: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      version: 1,
    }),
  },
}));

// Prevent navigation side effects.
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return { ...actual, useNavigate: () => vi.fn() };
});

describe("NewClaimPage", () => {
  it("shows required-field errors when submitted empty", async () => {
    const user = userEvent.setup();
    renderWithProviders(<NewClaimPage />);

    await user.click(screen.getByRole("button", { name: /submit claim/i }));

    await waitFor(() => {
      expect(screen.getByText(/policy number is required/i)).toBeInTheDocument();
    });
  });

  it("rejects a malformed policy number", async () => {
    const user = userEvent.setup();
    renderWithProviders(<NewClaimPage />);

    await user.type(screen.getByLabelText(/policy number/i), "BAD-123");
    await user.click(screen.getByRole("button", { name: /submit claim/i }));

    await waitFor(() => {
      expect(screen.getByText(/POL-MT-######/)).toBeInTheDocument();
    });
  });
});