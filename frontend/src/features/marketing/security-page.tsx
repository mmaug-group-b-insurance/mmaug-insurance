import { PublicLayout } from "./public-layout";
import { Badge } from "@/components/ui/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Lock, Users, Eye, FileSearch, ScrollText, ShieldCheck, KeyRound, Database } from "lucide-react";

const items = [
  { icon: Lock, title: "Identity", description: "Authentication delegated to Microsoft Entra ID via OAuth2 / OpenID Connect. The frontend never handles passwords." },
  { icon: Users, title: "Authorisation", description: "Server-side role checks per endpoint. The UI hides actions the user cannot perform; the server refuses them regardless." },
  { icon: KeyRound, title: "Secrets", description: "Backend secrets are stored in Azure Key Vault and accessed via managed identity. The frontend contains only public configuration." },
  { icon: Eye, title: "Document isolation", description: "Uploaded files are quarantined, scanned, and only clean documents are extracted. Document text is treated as data, never executed." },
  { icon: FileSearch, title: "Evidence traceability", description: "Findings and summary statements link to a document page, tool call, or rule ID. Statements without evidence are dropped." },
  { icon: ScrollText, title: "Audit logging", description: "Insert-only, hash-chained audit events. The database role for the application lacks UPDATE and DELETE on audit tables." },
  { icon: ShieldCheck, title: "PII protection", description: "PII columns are encrypted at rest. Masks are applied in logs and in prompts sent to the model." },
  { icon: Database, title: "Retention", description: "Retention policies per entity type define retention days and expiry action (delete or anonymise)." },
];

export function SecurityPage() {
  return (
    <PublicLayout>
      <section className="border-b">
        <div className="container-page py-16">
          <Badge variant="muted">Security & Privacy</Badge>
          <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight sm:text-5xl">
            Controls we implement — nothing we merely claim.
          </h1>
          <p className="mt-4 max-w-2xl text-base text-muted-foreground">
            No third-party certifications or compliance statuses are asserted on this page. The list
            below describes capabilities actually implemented in the platform and its deployment.
          </p>
        </div>
      </section>
      <section className="border-b">
        <div className="container-page grid gap-6 py-16 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((i) => (
            <Card key={i.title}>
              <CardHeader>
                <i.icon className="h-5 w-5 text-primary" />
                <CardTitle className="mt-3 text-base">{i.title}</CardTitle>
                <CardDescription>{i.description}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </section>
    </PublicLayout>
  );
}