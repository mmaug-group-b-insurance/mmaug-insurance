export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1",
  entra: {
    clientId: import.meta.env.VITE_ENTRA_CLIENT_ID ?? "",
    tenantId: import.meta.env.VITE_ENTRA_TENANT_ID ?? "",
    authority: import.meta.env.VITE_ENTRA_AUTHORITY ?? "https://login.microsoftonline.com/common",
    redirectUri: import.meta.env.VITE_ENTRA_REDIRECT_URI ?? window.location.origin,
    apiScope: import.meta.env.VITE_ENTRA_API_SCOPE ?? "",
  },
  envName: import.meta.env.VITE_ENV_NAME ?? "Local",
  appVersion: import.meta.env.VITE_APP_VERSION ?? "0.0.0",
  isDev: import.meta.env.DEV,
} as const;