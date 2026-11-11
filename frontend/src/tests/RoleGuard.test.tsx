import { describe, it, expect, vi } from "vitest";
import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router-dom";
import { renderWithProviders } from "./test-utils";
import { RoleGuard } from "@/features/auth/role-guard";

vi.mock("@/features/auth/use-auth", () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from "@/features/auth/use-auth";
const mockedUseAuth = vi.mocked(useAuth);

function setAuth(overrides: Partial<ReturnType<typeof useAuth>>) {
  mockedUseAuth.mockReturnValue({
    account: null,
    roles: [],
    isAuthenticated: false,
    isLoading: false,
    isBypass: false,
    profileUnavailable: false,
    retryProfile: vi.fn(),
    signIn: vi.fn(),
    signOut: vi.fn(),
    hasRole: () => false,
    ...overrides,
  });
}

describe("RoleGuard", () => {
  it("redirects to /login when not authenticated", () => {
    setAuth({ isAuthenticated: false });
    renderWithProviders(
      <Routes>
        <Route path="/app" element={<RoleGuard><div>protected</div></RoleGuard>} />
        <Route path="/login" element={<div>Sign in target</div>} />
      </Routes>,
      { initialEntries: ["/app"] }
    );
    expect(screen.queryByText("protected")).not.toBeInTheDocument();
    expect(screen.getByText("Sign in target")).toBeInTheDocument();
  });

  it("renders children when authenticated and no roles required", () => {
    setAuth({ isAuthenticated: true, hasRole: () => true });
    renderWithProviders(
      <RoleGuard>
        <div>protected</div>
      </RoleGuard>
    );
    expect(screen.getByText("protected")).toBeInTheDocument();
  });

  it("blocks when role missing", () => {
    setAuth({ isAuthenticated: true, hasRole: () => false, roles: ["CLAIMANT"] });
    renderWithProviders(
      <Routes>
        <Route path="/app" element={<RoleGuard roles={["CLAIMS_OFFICER"]}><div>officer-only</div></RoleGuard>} />
        <Route path="/403" element={<div>Forbidden target</div>} />
      </Routes>,
      { initialEntries: ["/app"] }
    );
    expect(screen.queryByText("officer-only")).not.toBeInTheDocument();
    expect(screen.getByText("Forbidden target")).toBeInTheDocument();
  });

  it("renders for a user with the required role", () => {
    setAuth({ isAuthenticated: true, hasRole: () => true, roles: ["CLAIMS_OFFICER"] });
    renderWithProviders(
      <RoleGuard roles={["CLAIMS_OFFICER"]}>
        <div>officer-only</div>
      </RoleGuard>
    );
    expect(screen.getByText("officer-only")).toBeInTheDocument();
  });
});
