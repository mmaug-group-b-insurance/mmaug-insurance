import { Link, useLocation } from "react-router-dom";
import { FileQuestion } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/use-auth";

export function NotFoundPage() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const isAppRoute = location.pathname.startsWith("/app");

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-muted">
        <FileQuestion className="h-7 w-7 text-muted-foreground" />
      </div>
      <h1 className="mt-6 text-2xl font-semibold tracking-tight">Page not found</h1>
      <p className="mt-2 max-w-md text-sm text-muted-foreground">
        The page you requested does not exist, or you may not have access to it.
      </p>
      <div className="mt-6 flex gap-3">
        <Link to={isAuthenticated ? (isAppRoute ? "/app" : "/") : "/"}>
          <Button>{isAuthenticated && isAppRoute ? "Go to dashboard" : "Go to homepage"}</Button>
        </Link>
      </div>
    </div>
  );
}