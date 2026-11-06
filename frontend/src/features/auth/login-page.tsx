// import { useEffect, useState } from "react";
// import { useNavigate, useLocation, Link } from "react-router-dom";
// import { useMsal } from "@azure/msal-react";
// import { ShieldCheck, LogIn, AlertCircle } from "lucide-react";
// import { Button } from "@/components/ui/button";
// import { Alert, AlertDescription } from "@/components/ui/alert";
// import { loginRequest } from "@/lib/auth";
// import { useAuth } from "./use-auth";
// import { env } from "@/lib/env";

// export function LoginPage() {
//   const { instance } = useMsal();
//   const { isAuthenticated } = useAuth();
//   const navigate = useNavigate();
//   const location = useLocation();
//   const from = (location.state as { from?: string } | null)?.from ?? "/app";
//   const [busy, setBusy] = useState(false);
//   const [error, setError] = useState<string | null>(null);

//   useEffect(() => {
//     if (isAuthenticated) navigate(from, { replace: true });
//   }, [isAuthenticated, navigate, from]);

//   const handleSignIn = async () => {
//     setError(null);
//     setBusy(true);
//     try {
//       await instance.loginRedirect(loginRequest);
//     } catch (e) {
//       setError(e instanceof Error ? e.message : "Sign-in failed. Please try again.");
//       setBusy(false);
//     }
//   };

//   return (
//     <div className="grid min-h-screen lg:grid-cols-2">
//       <div className="relative hidden flex-col justify-between bg-primary p-10 text-primary-foreground lg:flex">
//         <Link to="/" className="text-lg font-semibold tracking-tight">MMAUG Insurance Claims Agent</Link>
//         <div className="space-y-4">
//           <h1 className="text-3xl font-semibold leading-tight">
//             Evidence-linked claims intelligence, with human oversight.
//           </h1>
//           <p className="max-w-md text-sm text-primary-foreground/80">
//             Sign in with your organisational Microsoft account to access the claims portal. Access is
//             role-based and every action is auditable.
//           </p>
//         </div>
//         <p className="text-xs text-primary-foreground/70">© {new Date().getFullYear()} MMAUG Insurance Ltd (simulation).</p>
//       </div>

//       <div className="flex flex-col justify-center px-6 py-12 sm:px-12">
//         <div className="mx-auto w-full max-w-sm space-y-6">
//           <div className="lg:hidden">
//             <Link to="/" className="text-base font-semibold tracking-tight">MMAUG Insurance Claims Agent</Link>
//           </div>

//           <div>
//             <h2 className="text-2xl font-semibold tracking-tight">Sign in</h2>
//             <p className="mt-1 text-sm text-muted-foreground">
//               Use your Microsoft Entra ID account. Passwords are never entered into this application.
//             </p>
//           </div>

//           {env.envName !== "Production" && (
//             <Alert variant="info">
//               <AlertCircle />
//               <AlertDescription>Environment: <strong>{env.envName}</strong> — not for production use.</AlertDescription>
//             </Alert>
//           )}

//           {error && (
//             <Alert variant="destructive">
//               <AlertCircle />
//               <AlertDescription>{error}</AlertDescription>
//             </Alert>
//           )}

//           <Button onClick={handleSignIn} disabled={busy} size="lg" className="w-full">
//             <LogIn />
//             {busy ? "Redirecting…" : "Sign in with Microsoft"}
//           </Button>

//           <div className="flex items-center gap-2 text-xs text-muted-foreground">
//             <ShieldCheck className="h-3.5 w-3.5" />
//             <span>Your session is protected by Microsoft Entra ID. All actions are recorded in the audit log.</span>
//           </div>
//         </div>
//       </div>
//     </div>
//   );
// }



import { useState } from "react";
import { useNavigate, useLocation, Link, Navigate } from "react-router-dom";
import { useMsal } from "@azure/msal-react";
import { ShieldCheck, LogIn, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { loginRequest } from "@/lib/auth";
import { useAuth } from "./use-auth";
import { env } from "@/lib/env";

export function LoginPage() {
  const { instance } = useMsal();
  const { isAuthenticated } = useAuth();
  useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/app";
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // If the user is already authenticated (including via the dev bypass),
  // redirect immediately at render time. This removes the window during
  // which the "Sign in with Microsoft" button is clickable.
  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  const handleSignIn = async () => {
    setError(null);
    setBusy(true);
    try {
      await instance.loginRedirect(loginRequest);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Sign-in failed. Please try again.");
      setBusy(false);
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between bg-primary p-10 text-primary-foreground lg:flex">
        <Link to="/" className="text-lg font-semibold tracking-tight">MMAUG Insurance Claims Agent</Link>
        <div className="space-y-4">
          <h1 className="text-3xl font-semibold leading-tight">
            Evidence-linked claims intelligence, with human oversight.
          </h1>
          <p className="max-w-md text-sm text-primary-foreground/80">
            Sign in with your organisational Microsoft account to access the claims portal. Access is
            role-based and every action is auditable.
          </p>
        </div>
        <p className="text-xs text-primary-foreground/70">© {new Date().getFullYear()} MMAUG Insurance Ltd (simulation).</p>
      </div>

      <div className="flex flex-col justify-center px-6 py-12 sm:px-12">
        <div className="mx-auto w-full max-w-sm space-y-6">
          <div className="lg:hidden">
            <Link to="/" className="text-base font-semibold tracking-tight">MMAUG Insurance Claims Agent</Link>
          </div>

          <div>
            <h2 className="text-2xl font-semibold tracking-tight">Sign in</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Use your Microsoft Entra ID account. Passwords are never entered into this application.
            </p>
          </div>

          {env.envName !== "Production" && (
            <Alert variant="info">
              <AlertCircle />
              <AlertDescription>Environment: <strong>{env.envName}</strong> — not for production use.</AlertDescription>
            </Alert>
          )}

          {error && (
            <Alert variant="destructive">
              <AlertCircle />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <Button onClick={handleSignIn} disabled={busy} size="lg" className="w-full">
            <LogIn />
            {busy ? "Redirecting…" : "Sign in with Microsoft"}
          </Button>

          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>Your session is protected by Microsoft Entra ID. All actions are recorded in the audit log.</span>
          </div>
        </div>
      </div>
    </div>
  );
}
