import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiRequestError } from "@/lib/api-client";
import { systemService } from "@/services/system";
import { useAccount, useMsal } from "@azure/msal-react";
import type { AccountInfo } from "@azure/msal-browser";
import {
  BYPASS_ACCOUNT,
  isAuthBypassEnabled,
  loginRequest,
} from "@/lib/auth";
import type { Role } from "@/types/common";

export interface AuthState {
  account: AccountInfo | null;
  roles: Role[];
  isAuthenticated: boolean;
  isLoading: boolean;
  profileUnavailable: boolean;
  retryProfile: () => void;
  signIn: () => void;
  signOut: () => void;
  hasRole: (allowed: Role[] | undefined) => boolean;
  isBypass: boolean;
}

export function useAuth(): AuthState {
  const { instance, inProgress } = useMsal();
  const activeAccount = useAccount() || instance.getActiveAccount() || instance.getAllAccounts()[0] || null;
  const account: AccountInfo | null = isAuthBypassEnabled ? BYPASS_ACCOUNT : activeAccount;
  const isAuthenticated = Boolean(account);
  const queryClient = useQueryClient();
  const profile = useQuery({
    queryKey: ["me", account?.homeAccountId],
    queryFn: ({ signal }) => systemService.me(signal),
    enabled: isAuthenticated && !isAuthBypassEnabled && inProgress === "none",
  });
  const roles: Role[] = isAuthBypassEnabled
    ? (BYPASS_ACCOUNT.idTokenClaims?.roles as Role[])
    : (profile.data?.roles && profile.data.roles.length > 0)
      ? profile.data.roles
      : (isAuthenticated ? ["CLAIMS_OFFICER"] : []);

  const signIn = () => instance.loginRedirect(loginRequest);

  const signOut = () => {
    queryClient.clear();
    if (isAuthBypassEnabled) {
      sessionStorage.removeItem("dev-bypass-role");
      window.location.assign("/");
      return;
    }
    return instance.logoutRedirect({ account: activeAccount, postLogoutRedirectUri: window.location.origin });
  };

  const hasRole = (allowed: Role[] | undefined) => {
    if (!allowed || allowed.length === 0) return true;
    return allowed.some((r) => roles.includes(r));
  };

  return {
    account,
    roles,
    isAuthenticated,
    isLoading: !isAuthBypassEnabled && (inProgress !== "none" || (isAuthenticated && profile.isPending)),
    profileUnavailable: profile.isError && !(profile.error instanceof ApiRequestError && [401, 403].includes(profile.error.status)),
    retryProfile: () => { void profile.refetch(); },
    signIn,
    signOut,
    hasRole,
    isBypass: isAuthBypassEnabled,
  };
}
