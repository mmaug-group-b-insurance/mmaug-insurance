import { PublicLayout } from "./public-layout";
import { Badge } from "@/components/ui/badge";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";

const faqs = [
  { q: "Does the AI approve or reject claims?", a: "No. The platform contains no APPROVED, REJECTED, or PAID claim state. The AI produces typed statements with evidence; officers record their review outcome. Approval, rejection, and payment remain outside the agent." },
  { q: "What documents are accepted?", a: "PDF, JPG, JPEG, PNG, and TIFF. Maximum 15 MB per file, maximum 20 files per claim. The backend re-validates everything the client sends." },
  { q: "How does the platform handle prompt injection?", a: "Document text is treated as data. Heuristics and a classifier flag suspected injection. Detected injection produces a critical finding and escalation recommendation; instructions in documents are never followed." },
  { q: "How is evidence traced?", a: "Every summary statement carries evidence references — document ID and page, tool call result, or business rule ID. The backend verifier drops or downgrades statements without valid sources." },
  { q: "What happens when a tool is unavailable?", a: "The run completes with a TOOL_UNAVAILABLE warning, an informational finding is raised, and the summary recommends officer review. The run status becomes COMPLETED_WITH_WARNINGS." },
  { q: "What review outcomes exist?", a: "ACKNOWLEDGED, INFO_REQUESTED, ESCALATED_TO_SENIOR, ACCEPTED_FOR_INVESTIGATION, SUMMARY_REJECTED_REGENERATE. Accepting for investigation is not approval of payment." },
  { q: "How do users access the portal?", a: "Access is through Microsoft Entra ID. Users are provisioned by their organisation; there is no self-service registration." },
  { q: "Is data retained indefinitely?", a: "No. Retention policies per entity type define retention days and expiry action. Auditors and administrators can review retention configuration as permitted." },
  { q: "Can officers edit AI statements?", a: "Yes — corrections are recorded with the original text, the edited text, and a reason. Edits are visible in the audit history." },
  { q: "Which roles exist?", a: "CLAIMANT, CLAIMS_OFFICER, AUDITOR, ADMIN, and a machine-only SERVICE_AGENT. Each role sees only what the backend authorises." },
];

export function FAQPage() {
  return (
    <PublicLayout>
      <section className="border-b">
        <div className="container-page py-16">
          <Badge variant="muted">FAQ</Badge>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
            Frequently asked questions.
          </h1>
        </div>
      </section>
      <section>
        <div className="container-page py-12">
          <Accordion type="single" collapsible className="mx-auto max-w-3xl">
            {faqs.map((f, i) => (
              <AccordionItem key={i} value={`q-${i}`}>
                <AccordionTrigger className="text-left">{f.q}</AccordionTrigger>
                <AccordionContent className="text-muted-foreground">{f.a}</AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>
      </section>
    </PublicLayout>
  );
}