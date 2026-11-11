import {
  PublicClientApplication,
  type Configuration,
  type AccountInfo,
  InteractionRequiredAuthError,
} from "@azure/msal-browser";
import { env } from "./env";
import type { Role } from "@/types/common";

if (!env.entra.clientId || !env.entra.tenantId) {
  console.warn("[auth] Missing VITE_ENTRA_CLIENT_ID / VITE_ENTRA_TENANT_ID. Authentication is disabled.");
}

const ALL_ROLES: Role[] = ["CLAIMANT", "CLAIMS_OFFICER", "AUDITOR", "ADMIN"];

// import.meta.env.DEV is false in production builds, so bypass can never be on there.
export const isAuthBypassEnabled =
  import.meta.env.DEV && import.meta.env.VITE_DEV_BYPASS_AUTH === "true";

function resolveBypassRole(): Role {
  const fromUrl = new URLSearchParams(window.location.search).get("role")?.toUpperCase();
  if (fromUrl && ALL_ROLES.includes(fromUrl as Role)) {
    sessionStorage.setItem("dev-bypass-role", fromUrl);
  }
  const stored = sessionStorage.getItem("dev-bypass-role");
  if (stored && ALL_ROLES.includes(stored as Role)) return stored as Role;
  return (import.meta.env.VITE_DEV_BYPASS_ROLE as Role | undefined) ?? "CLAIMS_OFFICER";
}

const BYPASS_ROLE = resolveBypassRole();

export const BYPASS_ACCOUNT = {
  homeAccountId: "dev-bypass",
  environment: "dev",
  tenantId: "dev",
  username: `dev.${BYPASS_ROLE.toLowerCase()}@local`,
  localAccountId: "dev-bypass",
  name: `Dev ${BYPASS_ROLE.replace(/_/g, " ")}`,
  idTokenClaims: { roles: [BYPASS_ROLE] },
} as AccountInfo;

if (isAuthBypassEnabled) {
  console.warn(`[auth] DEV BYPASS ENABLED — role ${BYPASS_ROLE}. Do not use in production.`);
}

export const msalConfig: Configuration = {
  auth: {
    clientId: env.entra.clientId || "00000000-0000-0000-0000-000000000000",
    authority: env.entra.authority,
    redirectUri: env.entra.redirectUri,
    postLogoutRedirectUri: env.entra.redirectUri,
  },
  cache: { cacheLocation: "sessionStorage", storeAuthStateInCookie: false },
};

export const loginRequest = {
  scopes: ["openid", "profile", "email", env.entra.apiScope].filter(Boolean),
};

export const msalInstance = new PublicClientApplication(msalConfig);

export async function acquireApiToken(): Promise<string | null> {
  if (isAuthBypassEnabled) return null;

  const account = msalInstance.getActiveAccount();
  if (!account) return null;
  try {
    const result = await msalInstance.acquireTokenSilent({ ...loginRequest, account });
    return result.accessToken;
  } catch (err) {
    if (err instanceof InteractionRequiredAuthError) {
      await msalInstance.acquireTokenRedirect({ ...loginRequest, account });
    }
    return null;
  }
}

export async function safeHandleRedirect(): Promise<void> {
  if (isAuthBypassEnabled) return;
  const result = await msalInstance.handleRedirectPromise();
  if (result?.account) msalInstance.setActiveAccount(result.account);
  if (!msalInstance.getActiveAccount()) {
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length === 1) msalInstance.setActiveAccount(accounts[0]);
  }
}