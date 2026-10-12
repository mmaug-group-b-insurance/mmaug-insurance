import { PublicLayout } from "./public-layout";
import {
  FileText, Layers, Search, Bot, ClipboardCheck, Activity, Users, Lock, Eye, ScrollText,
} from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

const groups = [
  {
    title: "Claims intake",
    items: [
      { icon: FileText, title: "Structured claim creation", description: "Policy number, claimant contacts, vehicle, incident details, and estimated amount. Backend-validated field rules." },
      { icon: Layers, title: "Multi-document upload", description: "PDF, JPG, JPEG, PNG, TIFF. Up to 15 MB per file, 20 files per claim. Type and size validated on both client and server." },
    ],
  },
  {
    title: "Document processing",
    items: [
      { icon: Search, title: "Extraction with provenance", description: "Each extracted field carries a confidence, page number, and snippet. Low-confidence extractions are flagged, not silently accepted." },
      { icon: Bot, title: "Fact consolidation", description: "Per-document extracted values are merged into claim facts with provenance and alternate values preserved." },
    ],
  },
  {
    title: "Evidence & findings",
    items: [
      { icon: Search, title: "Missing & contradictory detection", description: "Rule engine and optional LLM pass detect missing required fields, cross-document contradictions, and coverage mismatches." },
      { icon: Eye, title: "Evidence tracing", description: "Each finding and statement links to the document, page, tool call, or rule that supports it." },
    ],
  },
  {
    title: "Summary & review",
    items: [
      { icon: Bot, title: "Evidence-linked AI summary", description: "Statements are typed and sectioned. Statements without valid evidence are dropped or downgraded." },
      { icon: ClipboardCheck, title: "Human review workflow", description: "Acknowledge, request information, escalate to senior, accept for investigation, or request regeneration. No approval or rejection." },
    ],
  },
  {
    title: "Operations",
    items: [
      { icon: Activity, title: "Auditable agent runs", description: "Every pipeline step and tool call is recorded with input/output summaries, durations, and error codes." },
      { icon: ScrollText, title: "Tamper-evident audit log", description: "Insert-only, hash-chained events. Filters by actor, action, outcome, claim, correlation ID." },
      { icon: Users, title: "Role-based access", description: "Claimants see their own claims. Officers see assigned and all claims per policy. Auditors see audit-relevant records." },
      { icon: Lock, title: "Security controls", description: "Entra ID authentication, PII masking, untrusted document isolation, and no autonomous decision authority." },
    ],
  },
];

export function FeaturesPage() {
  return (
    <PublicLayout>
      <section className="border-b">
        <div className="container-page py-16">
          <Badge variant="muted">Capabilities</Badge>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
            Designed for claims teams, audited by design.
          </h1>
          <p className="mt-4 max-w-2xl text-base text-muted-foreground">
            Every capability below exists in the platform and is enforced by the backend. The interface
            never substitutes for server-side authorisation.
          </p>
        </div>
      </section>

      {groups.map((g) => (
        <section key={g.title} className="border-b">
          <div className="container-page py-12">
            <h2 className="text-lg font-semibold tracking-tight">{g.title}</h2>
            <div className="mt-6 grid gap-6 md:grid-cols-2">
              {g.items.map((i) => (
                <Card key={i.title}>
                  <CardHeader>
                    <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary">
                      <i.icon className="h-5 w-5" />
                    </div>
                    <CardTitle className="text-base">{i.title}</CardTitle>
                    <CardDescription>{i.description}</CardDescription>
                  </CardHeader>
                </Card>
              ))}
            </div>
          </div>
        </section>
      ))}
    </PublicLayout>
  );
}