import { PublicLayout } from "./public-layout";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CheckCircle2, Bot, UserCheck, ShieldAlert } from "lucide-react";

const pipeline = [
  { step: "1", type: "SCAN_CHECK", title: "Scan check", detail: "Only documents with scan_status CLEAN proceed. Infected or rejected files are excluded and flagged." },
  { step: "2", type: "OCR", title: "OCR", detail: "Document Intelligence returns text, tables, and page numbers. Raw text is stored in blob, not in the database." },
  { step: "3", type: "STRUCTURE", title: "Structure", detail: "The model fills a claim-facts schema from OCR text. Document content is treated as data only." },
  { step: "4", type: "GUARDRAIL", title: "Injection check", detail: "Heuristics and a classifier flag suspected prompt injection. Instructions in documents are never followed." },
  { step: "5", type: "CONSOLIDATE", title: "Consolidate", detail: "Per-document fields and the claim form are merged into a single claim-fact set with provenance." },
  { step: "6", type: "VALIDATE", title: "Validate", detail: "Rule engine detects missing fields, contradictions across sources, and low-confidence extractions." },
  { step: "7", type: "TOOL_CALL", title: "Tools", detail: "Read-only tools fetch policy, coverage, claim history, and business rules. Every call is logged." },
  { step: "8", type: "VALIDATE", title: "Compare", detail: "Claim facts are compared with policy facts — dates, vehicle, driver, coverage." },
  { step: "9", type: "GENERATE_SUMMARY", title: "Summarise", detail: "The model writes typed statements, each citing evidence IDs." },
  { step: "10", type: "VERIFY_EVIDENCE", title: "Verify", detail: "Statements without a valid source are dropped or downgraded before display." },
  { step: "11", type: "ESCALATION_CHECK", title: "Escalate", detail: "Rules and uncertainty produce a recommendation. Officers decide the review outcome." },
];

export function HowItWorksPage() {
  return (
    <PublicLayout>
      <section className="border-b">
        <div className="container-page py-16">
          <Badge variant="muted">Workflow</Badge>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
            Eleven steps. One human at the end.
          </h1>
          <p className="mt-4 max-w-2xl text-base text-muted-foreground">
            Automated processing is bounded and read-only. The pipeline never writes to policy systems,
            never authorises payment, and never changes a claim status to approved or rejected.
          </p>
        </div>
      </section>

      <section className="border-b">
        <div className="container-page grid gap-12 py-16 lg:grid-cols-2">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <Bot className="h-5 w-5 text-primary" /> Automated pipeline
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Bounded LLM calls sit inside a fixed pipeline. Tool access is allow-listed and read-only.
            </p>
            <ol className="mt-6 space-y-3">
              {pipeline.map((p) => (
                <li key={p.step} className="flex gap-3 rounded-md border bg-card p-3">
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">{p.step}</span>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">{p.title}</p>
                      <Badge variant="outline" className="font-mono text-[10px]">{p.type}</Badge>
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">{p.detail}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <UserCheck className="h-5 w-5 text-primary" /> Human review
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <p className="text-muted-foreground">
                  The officer reviews the summary, findings, and evidence. The available outcomes are:
                </p>
                <ul className="space-y-1.5">
                  {["ACKNOWLEDGED", "INFO_REQUESTED", "ESCALATED_TO_SENIOR", "ACCEPTED_FOR_INVESTIGATION", "SUMMARY_REJECTED_REGENERATE"].map((o) => (
                    <li key={o} className="flex items-center gap-2">
                      <CheckCircle2 className="h-3.5 w-3.5 text-success" />
                      <span className="font-mono text-xs">{o}</span>
                    </li>
                  ))}
                </ul>
                <p className="text-xs text-muted-foreground">
                  There is no approve, reject, or pay outcome. Accepting for investigation is not approval of payment.
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <ShieldAlert className="h-5 w-5 text-warning" /> Failure handling
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <p className="text-muted-foreground">
                  Tool failures, timeouts, and unavailable dependencies produce warnings and an
                  informational finding. The run status becomes COMPLETED_WITH_WARNINGS, never COMPLETED.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}