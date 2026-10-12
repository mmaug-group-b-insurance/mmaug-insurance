import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "./test-utils";
import { StatementItem } from "@/features/summary/statement-item";
import type { SummaryStatement } from "@/types/summary";

const base: SummaryStatement = {
  id: "s1",
  orderNo: 1,
  section: "OVERVIEW",
  type: "EXTRACTED_FACT",
  text: "The claim form records the incident date as 2025-06-01.",
  confidence: 0.93,
  evidence: [
    {
      sourceType: "DOCUMENT",
      documentId: "doc-1",
      documentName: "claim-form.pdf",
      page: 1,
    },
  ],
};

describe("StatementItem", () => {
  it("renders the statement type tag", () => {
    renderWithProviders(<StatementItem statement={base} />);
    expect(screen.getByText(/extracted fact/i)).toBeInTheDocument();
  });

  it("renders the statement text and confidence", () => {
    renderWithProviders(<StatementItem statement={base} />);
    expect(screen.getByText(/claim form records the incident date/i)).toBeInTheDocument();
    expect(screen.getByText(/0.93/)).toBeInTheDocument();
  });

  it("renders an evidence chip that opens a drawer when clicked", async () => {
    renderWithProviders(<StatementItem statement={base} />);
    const chip = screen.getByRole("button", { name: /view evidence/i });
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent(/claim-form\.pdf/);
  });

  it("distinguishes statement types visibly", () => {
    renderWithProviders(
      <StatementItem
        statement={{ ...base, type: "UNCERTAIN", text: "The repair estimate could not be corroborated." }}
      />
    );
    expect(screen.getByText(/uncertain/i)).toBeInTheDocument();
  });
});