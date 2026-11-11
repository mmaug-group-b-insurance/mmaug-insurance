import React from "react";
import ReactDOM from "react-dom/client";
import { queryClient } from "@/app/query-client";
import { EventType } from "@azure/msal-browser";
import {
  msalInstance,
  acquireApiToken,
  safeHandleRedirect,
} from "@/lib/auth";
import { setTokenProvider } from "@/lib/api-client";
import { AppProviders } from "@/app/providers";
import App from "./App";
import "./styles/global.css";

async function bootstrap() {
  try {
    await msalInstance.initialize();

    // Skip handleRedirectPromise when the dev bypass is active. Otherwise a
    // stale MSAL state from an earlier real-auth attempt would immediately
    // redirect to Microsoft on every page load.
    await safeHandleRedirect();

    msalInstance.addEventCallback((event) => {
      if (event.eventType === EventType.LOGIN_SUCCESS && event.payload) {
        const account = (event.payload as { account?: import("@azure/msal-browser").AccountInfo }).account;
        if (account) {
          if (account.homeAccountId !== msalInstance.getActiveAccount()?.homeAccountId) queryClient.clear();
          msalInstance.setActiveAccount(account);
        }
      }
    });

    setTokenProvider(acquireApiToken);
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("[msal] bootstrap error", err);
  }

  ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
      <AppProviders>
        <App />
      </AppProviders>
    </React.StrictMode>
  );
}

void bootstrap();