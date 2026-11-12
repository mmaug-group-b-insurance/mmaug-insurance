import {
  PublicClientApplication,
  type Configuration,
  type AccountInfo,
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

function isJwtExpired(token: string): boolean {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return false;
    const payload = JSON.parse(atob(parts[1]));
    if (!payload.exp) return false;
    return payload.exp <= Math.floor(Date.now() / 1000) + 30;
  } catch {
    return false;
  }
}

function getCachedToken(account: AccountInfo): string | null {
  if ((account as { idToken?: string }).idToken) {
    const t = (account as { idToken?: string }).idToken!;
    if (!isJwtExpired(t)) return t;
  }
  try {
    let fallbackIdToken: string | null = null;
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i);
      if (!key) continue;
      if (key.includes(account.homeAccountId)) {
        if (key.includes("accesstoken")) {
          const item = sessionStorage.getItem(key);
          if (item) {
            const parsed = JSON.parse(item);
            if (parsed.secret && !isJwtExpired(parsed.secret)) {
              return parsed.secret;
            }
          }
        } else if (key.includes("idtoken")) {
          const item = sessionStorage.getItem(key);
          if (item) {
            const parsed = JSON.parse(item);
            if (parsed.secret && !isJwtExpired(parsed.secret)) {
              fallbackIdToken = parsed.secret;
            }
          }
        }
      }
    }
    if (fallbackIdToken) return fallbackIdToken;
  } catch {
    // Ignore storage access/parse errors
  }
  return null;
}

const withTimeout = <T>(promise: Promise<T>, ms = 2000): Promise<T> =>
  Promise.race([
    promise,
    new Promise<T>((_, reject) => setTimeout(() => reject(new Error("Token silent timeout")), ms)),
  ]);

export async function acquireApiToken(): Promise<string | null> {
  if (isAuthBypassEnabled) return null;

  const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
  if (!account) return null;

  // 1. Check local session cache first (instant, 0ms)
  const cached = getCachedToken(account);
  if (cached) return cached;

  // 2. Try silent acquisition with apiScope
  if (env.entra.apiScope) {
    try {
      const result = await withTimeout(
        msalInstance.acquireTokenSilent({
          scopes: [env.entra.apiScope],
          account,
        })
      );
      if (result?.accessToken) return result.accessToken;
      if (result?.idToken) return result.idToken;
    } catch {
      // Fallback if custom scope fails or times out
    }
  }

  // 3. Try standard openid scopes
  try {
    const result = await withTimeout(
      msalInstance.acquireTokenSilent({
        scopes: ["openid", "profile", "email"],
        account,
      })
    );
    if (result?.accessToken) return result.accessToken;
    if (result?.idToken) return result.idToken;
  } catch {
    // Fallback
  }

  return getCachedToken(account);
}

export async function safeHandleRedirect(): Promise<void> {
  if (isAuthBypassEnabled) return;
  const result = await msalInstance.handleRedirectPromise();
  if (result?.account) msalInstance.setActiveAccount(result.account);
  if (!msalInstance.getActiveAccount()) {
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0) msalInstance.setActiveAccount(accounts[0]);
  }
}