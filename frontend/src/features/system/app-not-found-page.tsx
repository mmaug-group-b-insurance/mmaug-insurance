import { Link } from "react-router-dom";
import { FileQuestion } from "lucide-react";
import { Button } from "@/components/ui/button";

export function AppNotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-muted">
        <FileQuestion className="h-7 w-7 text-muted-foreground" />
      </div>
      <h1 className="mt-6 text-xl font-semibold">Not found</h1>
      <p className="mt-2 max-w-md text-sm text-muted-foreground">
        That route does not exist within the application.
      </p>
      <Link to="/app" className="mt-6"><Button>Back to dashboard</Button></Link>
    </div>
  );
}