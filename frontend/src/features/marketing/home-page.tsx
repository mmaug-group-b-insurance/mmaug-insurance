import { Link } from "react-router-dom";
import { motion } from "motion/react";
import {
  ArrowRight, FileSearch, ShieldCheck, Bot, ClipboardCheck, Activity, Layers, Search,
  AlertTriangle, GitBranch, ScrollText, Users, Lock, Eye, CheckCircle2, FileText,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Accordion, AccordionContent, AccordionItem, AccordionTrigger,
} from "@/components/ui/accordion";
import { PublicLayout } from "./public-layout";

const features = [
  { icon: FileText, title: "Structured claims intake", description: "Consistent claim creation with validation on policy number, incident date, and description length." },
  { icon: Layers, title: "Document processing & extraction", description: "PDF, JPG, JPEG, PNG and TIFF documents are processed to extract structured claim facts with page references." },
  { icon: Search, title: "Missing & contradictory information", description: "Rule-driven detection of missing fields, contradictions across documents, and low-confidence extractions." },
  { icon: Bot, title: "Evidence-linked AI summaries", description: "Every summary statement is typed (retrieved fact, extracted fact, AI observation, uncertain, recommended check) and cites its source." },
  { icon: ClipboardCheck, title: "Human review & escalation", description: "Officers acknowledge, request information, escalate, accept for investigation, or request regeneration — never approve or reject." },
  { icon: Activity, title: "Auditable agent execution", description: "Every pipeline step and tool call is recorded with versioned prompts, tool schemas, and business rules." },
  { icon: Users, title: "Role-based access", description: "Claimants, officers, auditors and admins see only what they are authorised to see. Server remains authoritative." },
  { icon: ShieldCheck, title: "Security by design", description: "Microsoft Entra ID authentication, untrusted document isolation, PII masking, and tamper-evident audit logs." },
];

const steps = [
  { n: 1, title: "Create or receive a claim", text: "Claimant or officer enters policy, incident, and claimant information. Backend validates and assigns a claim reference." },
  { n: 2, title: "Upload supporting documents", text: "Claim form, police report, repair estimate, photos. Files are quarantined, scanned, and only clean documents proceed." },
  { n: 3, title: "Extract and validate", text: "Document Intelligence and a structuring step populate a claim-facts schema. Rule engine flags missing or contradictory fields." },
  { n: 4, title: "Retrieve policy context", text: "Controlled read-only tools fetch policy, coverage, and claim history. All tool calls are logged with arguments and results." },
  { n: 5, title: "Generate evidence-linked summary", text: "Each statement is typed and must cite a valid source. Statements without valid evidence are dropped or downgraded." },
  { n: 6, title: "Authorised human review", text: "A claims officer reviews the summary, findings, and evidence. Outcomes exclude approval, rejection, and payment." },
  { n: 7, title: "Record outcome and audit history", text: "Every step, tool call, review, and correction is written to a tamper-evident audit log with hashed chain." },
];

export function HomePage() {
  return (
    <PublicLayout>
      <Hero />
      <Trust />
      <FeatureGrid />
      <HowItWorks />
      <ResponsibleAI />
      <SecuritySection />
      <ProductPreview />
      <FAQSection />
      <FinalCTA />
    </PublicLayout>
  );
}

function Hero() {
  return (
    <section className="relative overflow-hidden border-b">
      <div className="container-page grid gap-12 py-16 lg:grid-cols-2 lg:py-24">
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="flex flex-col justify-center"
        >
          <Badge variant="muted" className="w-fit">Decision-support only — never autonomous</Badge>
          <h1 className="mt-4 text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
            Process motor claims with traceable evidence and human oversight.
          </h1>
          <div className="mt-4 max-w-xl space-y-3 text-base text-muted-foreground">
            <p>
              The MMAUG Insurance Claims Agent is a solution built as part of the Malta Microsoft AI User Group (MMAUG) AI & DevOps Fundamentals Bootcamp 2026. The solution is for demo/educational purposes and not to be regarded as a production-ready system.
            </p>
            <p>
              The solution helps claims teams organise documents, surface missing information, and prepare evidence-linked summaries — while authorised officers remain accountable for material decisions.
            </p>
          </div>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/login">
              <Button size="lg">
                Access Claims Portal <ArrowRight />
              </Button>
            </Link>
            <Link to="/features">
              <Button size="lg" variant="outline">Explore Features</Button>
            </Link>
          </div>
          <ul className="mt-8 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
            <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-success" /> Evidence-linked summaries</li>
            <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-success" /> Missing & contradictory detection</li>
            <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-success" /> Role-based access</li>
            <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-success" /> Auditable agent runs</li>
          </ul>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="relative"
        >
          <HeroPreview />
        </motion.div>
      </div>
    </section>
  );
}

function HeroPreview() {
  return (
    <div className="relative rounded-xl border bg-card shadow-lg">
      <div className="flex items-center gap-2 border-b px-4 py-3">
        <div className="flex gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-destructive/60" />
          <span className="h-2.5 w-2.5 rounded-full bg-warning/60" />
          <span className="h-2.5 w-2.5 rounded-full bg-success/60" />
        </div>
        <span className="ml-2 text-xs text-muted-foreground">Illustrative preview — synthetic data</span>
      </div>
      <div className="grid gap-3 p-4">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">Claim</p>
            <p className="font-mono text-sm font-medium">CLM-2026-000123</p>
          </div>
          <Badge variant="warning">READY_FOR_REVIEW</Badge>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Stat label="Documents" value="4" />
          <Stat label="Open findings" value="2" />
          <Stat label="Confidence" value="MEDIUM" />
        </div>
        <div className="rounded-md border bg-muted/30 p-3">
          <p className="text-xs font-medium text-muted-foreground">Summary statement</p>
          <p className="mt-1 text-sm">
            Police report records incident date differing from the claim form.
          </p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            <Badge variant="info">EXTRACTED_FACT</Badge>
            <Badge variant="outline">police-report.pdf · p1</Badge>
            <Badge variant="outline">claim-form.pdf · p1</Badge>
          </div>
        </div>
        <div className="rounded-md border bg-warning/5 p-3">
          <p className="flex items-center gap-2 text-xs font-medium text-warning">
            <AlertTriangle className="h-3.5 w-3.5" /> Contradiction detected
          </p>
          <p className="mt-1 text-xs text-muted-foreground">CR-001 · incidentDate — claim form vs police report</p>
        </div>
        <div className="rounded-md border bg-muted/30 p-3">
          <p className="text-xs font-medium text-muted-foreground">Agent run</p>
          <div className="mt-2 flex items-center gap-2 text-xs">
            <GitBranch className="h-3.5 w-3.5 text-muted-foreground" />
            <span className="text-muted-foreground">Step 9/11 · GENERATE_SUMMARY</span>
            <Badge variant="success" className="ml-auto">RUNNING</Badge>
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border bg-card p-3">
      <p className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-0.5 text-sm font-semibold">{value}</p>
    </div>
  );
}

function Trust() {
  const items = [
    "Structured claims intake",
    "Document processing and extraction",
    "Missing and contradictory information detection",
    "Evidence-linked AI summaries",
    "Human review and escalation",
    "Auditable agent execution",
    "Role-based access",
  ];
  return (
    <section className="border-b bg-muted/30">
      <div className="container-page py-10">
        <p className="text-center text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Capabilities supported by the platform
        </p>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
          {items.map((t) => (
            <Badge key={t} variant="outline" className="bg-background text-xs">
              <ShieldCheck className="h-3 w-3" /> {t}
            </Badge>
          ))}
        </div>
      </div>
    </section>
  );
}

function FeatureGrid() {
  return (
    <section id="features" className="border-b">
      <div className="container-page py-20">
        <SectionHeading
          eyebrow="Capabilities"
          title="Everything an officer needs to investigate, nothing that oversteps."
          description="Each capability is designed to support human review — not to replace it."
        />
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f) => (
            <Card key={f.title} className="h-full">
              <CardHeader>
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary">
                  <f.icon className="h-5 w-5" />
                </div>
                <CardTitle className="text-base">{f.title}</CardTitle>
                <CardDescription>{f.description}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHeading({ eyebrow, title, description, className }: { eyebrow?: string; title: string; description?: string; className?: string }) {
  return (
    <div className={className}>
      {eyebrow && <p className="text-xs font-semibold uppercase tracking-wider text-primary">{eyebrow}</p>}
      <h2 className="mt-2 max-w-3xl text-3xl font-semibold tracking-tight sm:text-4xl">{title}</h2>
      {description && <p className="mt-3 max-w-2xl text-base text-muted-foreground">{description}</p>}
    </div>
  );
}

function HowItWorks() {
  return (
    <section id="how-it-works" className="border-b bg-muted/30">
      <div className="container-page py-20">
        <SectionHeading
          eyebrow="Workflow"
          title="A seven-step pipeline with a human at the end."
          description="Automated processing is clearly distinguished from human review. Officers decide — the agent prepares."
        />
        <ol className="mt-12 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {steps.map((s) => (
            <li key={s.n} className="relative rounded-lg border bg-card p-5">
              <div className="flex items-center gap-3">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
                  {s.n}
                </span>
                <h3 className="text-sm font-semibold">{s.title}</h3>
              </div>
              <p className="mt-3 text-sm text-muted-foreground">{s.text}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

function ResponsibleAI() {
  return (
    <section id="responsible-ai" className="border-b">
      <div className="container-page py-20">
        <div className="grid gap-12 lg:grid-cols-2">
          <div>
            <SectionHeading
              eyebrow="Responsible AI"
              title="AI assists. Officers decide."
              description="The platform never approves, rejects, determines liability, authorises payment, or modifies a policy."
            />
            <ul className="mt-8 space-y-4 text-sm">
              {[
                "Every summary statement is typed: retrieved fact, extracted fact, AI observation, uncertain, or recommended check.",
                "Each statement cites its evidence — a document page, a tool result, or a business rule.",
                "Uncertainty is surfaced explicitly; low-confidence extractions are not presented as confirmed facts.",
                "Escalation is recommended by rules and uncertainty, never silently decided by the model.",
                "Authorised human review is required before any material decision.",
              ].map((line) => (
                <li key={line} className="flex gap-3">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                  <span>{line}</span>
                </li>
              ))}
            </ul>
          </div>
          <Card className="self-start">
            <CardHeader>
              <CardTitle className="text-base">Fact vs AI — what you will see</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <LegendRow label="RETRIEVED_FACT" tone="info" text="A fact returned from a controlled read-only tool (policy, coverage, history)." />
              <LegendRow label="EXTRACTED_FACT" tone="success" text="A value extracted from a document, with page reference and confidence." />
              <LegendRow label="AI_OBSERVATION" tone="muted" text="A neutral observation generated by the model, always linked to evidence." />
              <LegendRow label="UNCERTAIN" tone="warning" text="Something that could not be confirmed; explicitly flagged." />
              <LegendRow label="RECOMMENDED_CHECK" tone="default" text="A suggested officer action — never a decision." />
            </CardContent>
          </Card>
        </div>
      </div>
    </section>
  );
}

function LegendRow({ label, tone, text }: { label: string; tone: "info" | "success" | "warning" | "muted" | "default"; text: string }) {
  return (
    <div className="flex gap-3">
      <Badge variant={tone as never} className="shrink-0 font-mono text-[10px]">{label}</Badge>
      <p className="text-xs text-muted-foreground">{text}</p>
    </div>
  );
}

function SecuritySection() {
  const items = [
    { icon: Lock, title: "Microsoft Entra ID", text: "Authentication delegated to your organisation's identity provider." },
    { icon: Users, title: "Role-based authorisation", text: "Claimant, officer, auditor, admin. The backend enforces every permission." },
    { icon: Eye, title: "Document protection", text: "Quarantine → scan → clean pipeline; untrusted text is never executed as instructions." },
    { icon: FileSearch, title: "Evidence traceability", text: "Each statement links to a document, page, tool result, or business rule." },
    { icon: ScrollText, title: "Audit logging", text: "Insert-only, hash-chained events; DB role cannot update or delete." },
    { icon: ShieldCheck, title: "PII protection", text: "Encrypted at rest, masked in logs and agent prompts." },
  ];
  return (
    <section id="security" className="border-b bg-muted/30">
      <div className="container-page py-20">
        <SectionHeading
          eyebrow="Security & Privacy"
          title="Built for insurance operations."
          description="Capabilities implemented in the platform — no certifications or third-party attestations are claimed."
        />
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((i) => (
            <div key={i.title} className="rounded-lg border bg-card p-5">
              <i.icon className="h-5 w-5 text-primary" />
              <h3 className="mt-3 text-sm font-semibold">{i.title}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{i.text}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ProductPreview() {
  return (
    <section className="border-b">
      <div className="container-page py-20">
        <SectionHeading
          eyebrow="Product preview"
          title="A claims intelligence workspace, not a black box."
          description="Illustrative interface with synthetic data. Real claims are only visible to authorised users."
        />
        <div className="mt-12 overflow-hidden rounded-xl border bg-card shadow-sm">
          <div className="grid gap-0 lg:grid-cols-[240px_1fr]">
            <aside className="hidden border-r bg-muted/30 p-4 lg:block">
              {[
                { label: "Dashboard", icon: Activity },
                { label: "Claims", icon: FileText, active: true },
                { label: "Audit Log", icon: ScrollText },
                { label: "About", icon: Bot },
              ].map((item) => (
                <div key={item.label} className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm ${item.active ? "bg-accent text-accent-foreground" : "text-muted-foreground"}`}>
                  <item.icon className="h-4 w-4" />
                  {item.label}
                </div>
              ))}
            </aside>
            <div className="p-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-semibold">Claims requiring review</h3>
                  <p className="text-xs text-muted-foreground">Illustrative — synthetic data only</p>
                </div>
                <div className="flex gap-2">
                  <Badge variant="muted">7 open</Badge>
                  <Badge variant="warning">3 escalated</Badge>
                </div>
              </div>
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground">
                      <th className="py-2 pr-4">Claim</th>
                      <th className="py-2 pr-4">Status</th>
                      <th className="py-2 pr-4">Findings</th>
                      <th className="py-2 pr-4">Updated</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {[
                      { id: "CLM-2026-000101", status: "READY_FOR_REVIEW", tone: "warning", findings: 2, updated: "12m ago" },
                      { id: "CLM-2026-000102", status: "ESCALATED", tone: "destructive", findings: 4, updated: "1h ago" },
                      { id: "CLM-2026-000103", status: "UNDER_INVESTIGATION", tone: "info", findings: 1, updated: "3h ago" },
                      { id: "CLM-2026-000104", status: "AWAITING_INFORMATION", tone: "muted", findings: 3, updated: "yesterday" },
                    ].map((r) => (
                      <tr key={r.id}>
                        <td className="py-2 pr-4 font-mono text-xs">{r.id}</td>
                        <td className="py-2 pr-4"><Badge variant={r.tone as never}>{r.status}</Badge></td>
                        <td className="py-2 pr-4">{r.findings}</td>
                        <td className="py-2 pr-4 text-xs text-muted-foreground">{r.updated}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function FAQSection() {
  const faqs = [
    { q: "Does the AI approve or reject claims?", a: "No. The platform contains no APPROVED, REJECTED, or PAID claim state. The AI produces typed statements with evidence; officers record their review outcome. Approval, rejection, and payment remain outside the agent." },
    { q: "How are uploaded documents handled?", a: "Documents enter a quarantine container, are scanned, and only clean files proceed to extraction. Document text is treated as data, never as instructions to the model." },
    { q: "How is evidence traced?", a: "Every summary statement carries evidence references — document ID and page, tool call result, or business rule ID. The backend verifier drops or downgrades statements without valid sources." },
    { q: "What happens when a tool is unavailable?", a: "The run completes with a TOOL_UNAVAILABLE warning, an informational finding is raised, and the summary recommends officer review. The run is never reported as fully successful." },
    { q: "How do users access the portal?", a: "Access is through Microsoft Entra ID. Users are provisioned by their organisation; there is no self-service registration." },
    { q: "How do I know the AI is not inventing facts?", a: "Statement types distinguish retrieved facts, extracted facts, AI observations, uncertain items, and recommended checks. Confidence and uncertainty are shown explicitly." },
  ];
  return (
    <section id="faq" className="border-b">
      <div className="container-page grid gap-12 py-20 lg:grid-cols-[1fr_2fr]">
        <SectionHeading eyebrow="FAQ" title="Questions officers ask first." />
        <Accordion type="single" collapsible className="w-full">
          {faqs.map((f, i) => (
            <AccordionItem key={i} value={`item-${i}`}>
              <AccordionTrigger className="text-left">{f.q}</AccordionTrigger>
              <AccordionContent className="text-muted-foreground">{f.a}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </section>
  );
}

function FinalCTA() {
  return (
    <section className="border-b bg-primary text-primary-foreground">
      <div className="container-page flex flex-col items-start justify-between gap-6 py-16 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">Access the claims portal</h2>
          <p className="mt-2 max-w-xl text-sm text-primary-foreground/80">
            Sign in with your organisational Microsoft account to continue.
          </p>
        </div>
        <Link to="/login">
          <Button size="lg" variant="secondary">
            Sign in <ArrowRight />
          </Button>
        </Link>
      </div>
    </section>
  );
}