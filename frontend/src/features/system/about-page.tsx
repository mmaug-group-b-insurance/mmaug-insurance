import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout";
import { systemService } from "@/services/system";
import { useAuth } from "@/features/auth/use-auth";
import { Info } from "lucide-react";

export function AboutPage() {
  const { account, roles } = useAuth();
  const query = useQuery({
    queryKey: ["versions"],
    queryFn: ({ signal }) => systemService.versions(signal),
    staleTime: 5 * 60_000,
    retry: 1,
  });

  return (
    <>
      <PageHeader title="About this system" description="Version information and current session." />

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Session</CardTitle>
            <CardDescription>Your identity as seen by the backend.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <Row label="Name" value={account?.name ?? "—"} />
            <Row label="Username" value={account?.username ?? "—"} />
            <Row
              label="Roles"
              value={
                <div className="flex flex-wrap gap-1">
                  {roles.length === 0 ? <span className="text-muted-foreground">No roles assigned</span> :
                    roles.map((r) => <Badge key={r} variant="muted" className="text-[10px]">{r}</Badge>)}
                </div>
              }
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">System versions</CardTitle>
            <CardDescription>Read from the backend. Not editable from the frontend.</CardDescription>
          </CardHeader>
          <CardContent>
            {query.isLoading ? (
              <div className="space-y-2">{[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-6 w-full" />)}</div>
            ) : query.isError ? (
              <>
                <Alert variant="info" className="mb-3">
                  <Info />
                  <AlertTitle>Version information unavailable</AlertTitle>
                  <AlertDescription>
                    The backend did not return version information. This does not affect other functionality.
                  </AlertDescription>
                </Alert>
                <ErrorAlert error={query.error} onRetry={() => query.refetch()} />
              </>
            ) : query.data ? (
              <div className="space-y-3 text-sm">
                <Row label="App version" value={<span className="font-mono">{query.data.appVersion}</span>} />
                <Row label="Git commit" value={<span className="font-mono">{query.data.gitCommit}</span>} />
                <Row label="Prompt version" value={<span className="font-mono">{query.data.promptVersion}</span>} />
                <Row label="Tool schema version" value={<span className="font-mono">{query.data.toolSchemaVersion}</span>} />
                <Row label="Rule set version" value={<span className="font-mono">{query.data.ruleSetVersion}</span>} />
                <Row label="Rule set SHA-256" value={<span className="break-all font-mono text-[10px]">{query.data.ruleSetSha256}</span>} />
              </div>
            ) : null}
          </CardContent>
        </Card>
      </div>

      <Card className="mt-4">
        <CardHeader>
          <CardTitle className="text-base">Responsible AI reminder</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          The AI produces typed statements with evidence. It does not approve, reject, determine
          liability, authorise payment, or modify policies. Authorised human officers record all
          consequential decisions.
        </CardContent>
      </Card>
    </>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[180px_1fr] gap-3">
      <span className="text-xs text-muted-foreground">{label}</span>
      <div className="text-sm">{value}</div>
    </div>
  );
}