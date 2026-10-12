import { AlertCircle, RefreshCcw } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "./alert";
import { Button } from "./button";
import { ApiRequestError } from "@/lib/api-client";

interface ErrorAlertProps {
  error: unknown;
  onRetry?: () => void;
  title?: string;
}

export function ErrorAlert({ error, onRetry, title = "Something went wrong" }: ErrorAlertProps) {
  const message =
    error instanceof ApiRequestError
      ? error.message
      : error instanceof Error
      ? error.message
      : "An unexpected error occurred.";

  const correlationId = error instanceof ApiRequestError ? error.correlationId : undefined;

  return (
    <Alert variant="destructive">
      <AlertCircle />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>
        <p>{message}</p>
        {correlationId && <p className="mt-1 font-mono text-xs opacity-80">Correlation ID: {correlationId}</p>}
        {onRetry && (
          <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>
            <RefreshCcw className="h-3.5 w-3.5" />
            Try again
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
}