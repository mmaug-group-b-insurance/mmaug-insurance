import { useEffect, useMemo, useState } from "react";
import { useMsal } from "@azure/msal-react";
import { EventType, type AccountInfo } from "@azure/msal-browser";
import {
  BYPASS_ACCOUNT,
  getUserRoles,
  isAuthBypassEnabled,
  loginRequest,
  msalInstance,
} from "@/lib/auth";
import type { Role } from "@/types/common";

function useAccounts(): AccountInfo[] {
  const [accounts, setAccounts] = useState<AccountInfo[]>(() => msalInstance.getAllAccounts());

  useEffect(() => {
    const refresh = () => setAccounts(msalInstance.getAllAccounts());

    const id = msalInstance.addEventCallback((event) => {
      const t = event.eventType;
      if (
        t === EventType.LOGIN_SUCCESS ||
        t === EventType.LOGOUT_SUCCESS ||
        t === EventType.ACQUIRE_TOKEN_SUCCESS ||
        t === EventType.HANDLE_REDIRECT_END
      ) {
        refresh();
      }
    });

    refresh();

    return () => {
      if (id) msalInstance.removeEventCallback(id);
    };
  }, []);

  return accounts;
}

export function useAuth() {
  const { instance, inProgress } = useMsal();
  const accounts = useAccounts();

  const account: AccountInfo | null = isAuthBypassEnabled ? BYPASS_ACCOUNT : accounts.at(0) ?? null;
  const isAuthenticated = Boolean(account);

  const roles = useMemo<Role[]>(() => getUserRoles(account) as Role[], [account]);

  const signIn = () => instance.loginRedirect(loginRequest);

  const signOut = () => {
    if (isAuthBypassEnabled) {
      sessionStorage.removeItem("dev-bypass-role");
      window.location.assign("/");
      return;
    }
    return instance.logoutRedirect({ postLogoutRedirectUri: window.location.origin });
  };

  const hasRole = (allowed: Role[] | undefined) => {
    if (!allowed || allowed.length === 0) return true;
    if (roles.includes("ADMIN")) return true;
    return allowed.some((r) => roles.includes(r));
  };

  return {
    account,
    roles,
    isAuthenticated,
    isLoading: !isAuthBypassEnabled && inProgress !== "none",
    signIn,
    signOut,
    hasRole,
    isBypass: isAuthBypassEnabled,
  };
}
