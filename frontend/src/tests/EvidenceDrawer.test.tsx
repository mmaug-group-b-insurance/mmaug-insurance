import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "./test-utils";
import { EvidenceDrawer } from "@/features/summary/evidence-drawer";

describe("EvidenceDrawer", () => {
  it("shows document source metadata and excerpt", () => {
    renderWithProviders(
      <EvidenceDrawer
        open
        onOpenChange={() => {}}
        evidence={{
          sourceType: "DOCUMENT",
          documentId: "d1",
          documentName: "police-report.pdf",
          page: 2,
          snippet: "Vehicle A collided with Vehicle B at approximately 14:20.",
        }}
      />
    );

    expect(screen.getByText(/police-report\.pdf/)).toBeInTheDocument();
    expect(screen.getByText(/Vehicle A collided/i)).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("shows tool result metadata for tool sources", () => {
    renderWithProviders(
      <EvidenceDrawer
        open
        onOpenChange={() => {}}
        evidence={{
          sourceType: "TOOL_RESULT",
          toolName: "get_policy",
          toolCallId: "call-42",
        }}
      />
    );
    expect(screen.getByText(/get_policy/)).toBeInTheDocument();
    expect(screen.getByText(/call-42/)).toBeInTheDocument();
  });
});