import { Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/layout";
import { RoleGuard } from "@/features/auth/role-guard";

// Public
import { HomePage } from "@/features/marketing/home-page";
import { FeaturesPage } from "@/features/marketing/features-page";
import { HowItWorksPage } from "@/features/marketing/how-it-works-page";
import { SecurityPage } from "@/features/marketing/security-page";
import { ResponsibleAIPage } from "@/features/marketing/responsible-ai-page";
import { FAQPage } from "@/features/marketing/faq-page";
import { LoginPage } from "@/features/auth/login-page";
import { NotFoundPage } from "@/features/marketing/not-found-page";
import { ForbiddenPage } from "@/features/marketing/forbidden-page";

// App (stubs for now — filled in Batches 8–9)
import { DashboardPage } from "@/features/dashboard/dashboard-page";
import { ClaimsListPage } from "@/features/claims/claims-list-page";
import { NewClaimPage } from "@/features/claims/new-claim-page";
import { ClaimDetailPage } from "@/features/claims/claim-detail-page";
import { AgentRunPage } from "@/features/agent-runs/agent-run-page";
import { AuditLogPage } from "@/features/audit/audit-log-page";
import { AboutPage } from "@/features/system/about-page";
import { AppNotFoundPage } from "@/features/system/app-not-found-page";

export function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<HomePage />} />
      <Route path="/features" element={<FeaturesPage />} />
      <Route path="/how-it-works" element={<HowItWorksPage />} />
      <Route path="/security" element={<SecurityPage />} />
      <Route path="/responsible-ai" element={<ResponsibleAIPage />} />
      <Route path="/faq" element={<FAQPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      {/* Authenticated app */}
      <Route
        path="/app"
        element={
          <RoleGuard>
            <AppLayout />
          </RoleGuard>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="claims" element={<ClaimsListPage />} />
        <Route
          path="claims/new"
          element={
            <RoleGuard roles={["CLAIMANT", "CLAIMS_OFFICER"]}>
              <NewClaimPage />
            </RoleGuard>
          }
        />
        <Route path="claims/:id" element={<ClaimDetailPage />} />
        <Route path="claims/:id/:tab" element={<ClaimDetailPage />} />
        <Route
          path="runs/:runId"
          element={
            <RoleGuard roles={["CLAIMS_OFFICER", "AUDITOR"]}>
              <AgentRunPage />
            </RoleGuard>
          }
        />
        <Route
          path="audit"
          element={
            <RoleGuard roles={["AUDITOR", "ADMIN"]}>
              <AuditLogPage />
            </RoleGuard>
          }
        />
        <Route
          path="about"
          element={
            <RoleGuard roles={["ADMIN", "CLAIMS_OFFICER"]}>
              <AboutPage />
            </RoleGuard>
          }
        />
        <Route path="403" element={<ForbiddenPage />} />
        <Route path="*" element={<AppNotFoundPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

// Also keep a default export so existing main.tsx (`import App from "./App"`) still works.
export default AppRoutes;