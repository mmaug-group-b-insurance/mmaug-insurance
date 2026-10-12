import { Link } from "react-router-dom";
import { ShieldX } from "lucide-react";
import { Button } from "@/components/ui/button";

export function ForbiddenPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-destructive/10">
        <ShieldX className="h-7 w-7 text-destructive" />
      </div>
      <h1 className="mt-6 text-2xl font-semibold tracking-tight">Access denied</h1>
      <p className="mt-2 max-w-md text-sm text-muted-foreground">
        You do not have permission to view this resource. If you believe this is an error, contact your
        administrator.
      </p>
      <div className="mt-6 flex gap-3">
        <Link to="/app"><Button>Go to dashboard</Button></Link>
        <Button variant="outline" onClick={() => history.back()}>Back</Button>
      </div>
    </div>
  );
}