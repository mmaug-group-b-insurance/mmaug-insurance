import { PublicLayout } from "./public-layout";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ShieldAlert, CheckCircle2, XCircle } from "lucide-react";

export function ResponsibleAIPage() {
  return (
    <PublicLayout>
      <section className="border-b">
        <div className="container-page py-16">
          <Badge variant="muted">Responsible AI</Badge>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
            What the agent does — and what it will never do.
          </h1>
        </div>
      </section>

      <section className="border-b">
        <div className="container-page grid gap-6 py-12 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <CheckCircle2 className="h-5 w-5 text-success" /> The agent may
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {[
                "Extract structured values from documents with page references.",
                "Merge per-document values into claim facts, preserving provenance.",
                "Retrieve policy, coverage, and claim history through read-only tools.",
                "Detect missing fields and cross-source contradictions.",
                "Generate typed statements, each citing evidence.",
                "Recommend escalation based on rules and uncertainty.",
              ].map((t) => (
                <p key={t} className="flex gap-2">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                  {t}
                </p>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <XCircle className="h-5 w-5 text-destructive" /> The agent will never
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {[
                "Approve or reject a claim.",
                "Determine final liability.",
                "Authorise payment.",
                "Modify an insurance policy.",
                "Silently assert facts without evidence.",
                "Follow instructions embedded in uploaded documents.",
              ].map((t) => (
                <p key={t} className="flex gap-2">
                  <XCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
                  {t}
                </p>
              ))}
            </CardContent>
          </Card>
        </div>
      </section>

      <section className="border-b">
        <div className="container-page space-y-8 py-12">
          <Alert variant="warning">
            <ShieldAlert />
            <AlertTitle>Uncertainty is surfaced, not hidden</AlertTitle>
            <AlertDescription>
              Statements typed <code className="font-mono text-xs">UNCERTAIN</code> and any low-confidence
              extractions are shown explicitly. The summary recommends officer review when uncertainty or
              escalation rules are triggered.
            </AlertDescription>
          </Alert>

          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-5">
            {[
              { type: "RETRIEVED_FACT", text: "Returned by a controlled read-only tool." },
              { type: "EXTRACTED_FACT", text: "Extracted from a document with page reference." },
              { type: "AI_OBSERVATION", text: "Neutral observation, always linked to evidence." },
              { type: "UNCERTAIN", text: "Could not be confirmed; explicitly flagged." },
              { type: "RECOMMENDED_CHECK", text: "A suggestion for the officer, not a decision." },
            ].map((s) => (
              <div key={s.type} className="rounded-lg border bg-card p-4">
                <Badge variant="outline" className="font-mono text-[10px]">{s.type}</Badge>
                <p className="mt-3 text-xs text-muted-foreground">{s.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}