# MMAUG Insurance Claims Agent — Frontend

React + TypeScript frontend for the **MMAUG Insurance Claims Agent**. Provides a public marketing
site and an authenticated claims-management application integrated with the Spring Boot backend
described in the project's file map and RFP.

The AI produces **typed, evidence-linked statements**. It never approves, rejects, determines
liability, authorises payment, or modifies policies. Authorised officers record all consequential
decisions.

> **Decision boundary.** There is no `APPROVED`, `REJECTED`, or `PAID` claim state anywhere in
> this application — not in buttons, menus, statuses, review outcomes, or API calls. The only
> permitted review outcomes are the five listed in [Responsible AI rules](#responsible-ai-rules).

---

## Table of contents

- [Stack](#stack)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Authentication](#authentication)
  - [Real Microsoft Entra ID sign-in](#real-microsoft-entra-id-sign-in)
  - [Entra ID app registration](#entra-id-app-registration)
  - [Development auth bypass](#development-auth-bypass)
  - [Switching between roles in development](#switching-between-roles-in-development)
- [Environment variables](#environment-variables)
- [Scripts](#scripts)
- [Application architecture](#application-architecture)
- [Route map](#route-map)
- [Roles and UI behaviour](#roles-and-ui-behaviour)
- [Feature reference](#feature-reference)
  - [Dashboard](#dashboard)
  - [Claims list](#claims-list)
  - [New claim](#new-claim)
  - [Document upload & extraction](#document-upload--extraction)
  - [Claim detail (tabs)](#claim-detail-tabs)
  - [AI Claim Summary](#ai-claim-summary)
  - [Human review](#human-review)
  - [Agent run details](#agent-run-details)
  - [Audit log](#audit-log)
  - [System information](#system-information)
- [Backend integration](#backend-integration)
- [Responsible AI rules](#responsible-ai-rules)
- [Testing](#testing)
- [Production build & deployment](#production-build--deployment)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)
- [Known limitations](#known-limitations)
- [Directory structure](#directory-structure)

---

## Stack

| Concern | Choice |
|---|---|
| Bundler | Vite 6 |
| UI | React 18 + TypeScript 5 |
| Styling | Tailwind CSS 3.4 + shadcn/ui primitives |
| Routing | React Router 6 |
| Server state | TanStack Query 5 |
| Forms | React Hook Form + Zod + `@hookform/resolvers` |
| Auth | Microsoft Entra ID via MSAL React |
| Icons | Lucide React |
| Charts | Recharts |
| Animation | Motion (`motion/react`) |
| Toasts | Sonner |
| Dates | date-fns |
| Tests | Vitest + React Testing Library + jsdom |

> **Assumption:** the prompt requested "latest stable Tailwind." Tailwind 4 is stable, but
> shadcn/ui's component recipes are currently most mature on Tailwind 3.4. This project pins
> Tailwind 3.4 to keep the component contracts stable. Migrating to v4 is a contained change.

---

## Requirements

- Node.js 20 LTS or later
- npm 10 or later (or pnpm/yarn — the lockfile in this repo ships for npm)
- A running MMAUG backend (Spring Boot) exposing the OpenAPI contract — optional for public
  pages, required for authenticated features to return real data
- A Microsoft Entra ID app registration — optional for local development (see
  [Development auth bypass](#development-auth-bypass))

---

## Getting started

```bash
git clone <your-repo-url>
cd frontend
npm install
cp .env.example .env
# edit .env with your local backend URL and, optionally, Entra configuration
npm run dev
```

The dev server runs on `http://localhost:5173`.

Open the browser:

- Public marketing site → `http://localhost:5173/`
- Authenticated application → `http://localhost:5173/app`

If you have not configured Entra ID yet, follow
[Development auth bypass](#development-auth-bypass) to preview the authenticated UI locally.

---

## Authentication

The frontend supports two authentication modes. Which one runs is controlled by a single
environment variable: `VITE_DEV_BYPASS_AUTH`.

### Real Microsoft Entra ID sign-in

This is the production path. When `VITE_DEV_BYPASS_AUTH` is unset or `false`, the app uses
MSAL Browser against your Entra ID tenant.

Flow:

1. Unauthenticated visit to `/app/*` → `RoleGuard` redirects to `/login` with
   `state.from` set to the original path.
2. On `/login`, clicking **Sign in with Microsoft** calls `instance.loginRedirect(loginRequest)`.
3. Microsoft authenticates and redirects back to `VITE_ENTRA_REDIRECT_URI`.
4. `main.tsx` runs `msalInstance.handleRedirectPromise()` → exchanges the code, sets the active
   account, and registers a login-success event listener.
5. `useAuth()` reads the account and its `roles` claim from the ID token.
6. `RoleGuard` lets the user through to the requested route.

The app never collects passwords. All credential handling is inside Microsoft's hosted login
page.

### Entra ID app registration

Register the SPA once per environment.

1. In [Entra ID](https://entra.microsoft.com) → **App registrations** → **+ New registration**.
2. Name: `MMAUG Claims Agent (Local)`.
3. Supported account types: **Accounts in this organizational directory only**.
4. Redirect URI:
   - Platform: **Single-page application (SPA)**
   - URL: `http://localhost:5173`
5. Register. Copy the **Application (client) ID** → `VITE_ENTRA_CLIENT_ID`.
6. Copy the **Directory (tenant) ID** → `VITE_ENTRA_TENANT_ID`.
7. Set `VITE_ENTRA_AUTHORITY=https://login.microsoftonline.com/<tenant-id>`.

**Expose an API scope** (needed when the backend validates a scope):

1. Create a second app registration for the API: `MMAUG Claims API`.
2. In that API app → **Expose an API** → **+ Add a scope** → name it `access_as_user`.
3. The full scope string is `api://<api-client-id>/access_as_user`.
4. In the SPA app → **API permissions** → **+ Add a permission** → **My APIs** → select the API
   → check `access_as_user` → **Grant admin consent**.
5. Set `VITE_ENTRA_API_SCOPE` to that full scope string.

**Define app roles**:

1. In the app registration that issues the token (SPA or API depending on your setup) →
   **App roles** → **+ Create app role** for each:
   - Display name: `Claimant`, Value: `CLAIMANT`, Allowed member types: **Users/Groups**
   - Display name: `Claims Officer`, Value: `CLAIMS_OFFICER`, Allowed member types: **Users/Groups**
   - Display name: `Auditor`, Value: `AUDITOR`, Allowed member types: **Users/Groups**
   - Display name: `Admin`, Value: `ADMIN`, Allowed member types: **Users/Groups**

The `Value` field is what appears in the `roles` claim of the ID token. `getUserRoles()` in
`src/lib/auth.ts` reads that claim.

**Assign users**:

1. Entra ID → **Enterprise applications** → search for your app → open it.
2. **Users and groups** → **+ Add user/group**.
3. Select a user and assign one of the roles.
4. Save. Sign out and back in for the token to refresh.

### Development auth bypass

For local UI work without configuring Entra ID, enable the bypass. It injects a fake account
into the MSAL instance at module load, so the frontend's `RoleGuard` treats you as
authenticated. It issues **no real access token**; backend calls go out unauthenticated and will
fail against a secured backend.

In `.env`:

```env
VITE_DEV_BYPASS_AUTH=true
VITE_DEV_BYPASS_ROLE=CLAIMS_OFFICER
```

Restart the dev server. Open `http://localhost:5173/app`. You land on the dashboard.

Console output should show:

```
[auth] Missing VITE_ENTRA_CLIENT_ID / VITE_ENTRA_TENANT_ID. Authentication is disabled.
[auth] DEV BYPASS ENABLED — impersonating role CLAIMS_OFFICER. Do not use in production.
```

**What the bypass does not do:**

- It does not issue a real access token. Backend calls have no `Authorization` header.
- It does not change backend authorization. The server is still the authority.
- It is not persisted to disk — the fake account lives in process memory only.
- It is a compile-time constant, so the fake user is tree-shaken out of production bundles when
  the flag is unset.

**To disable**, set `VITE_DEV_BYPASS_AUTH=false` (or remove the line) and restart.

### Switching between roles in development

The bypass role is fixed at boot. To preview a different role:

1. Edit `.env`:
   ```env
   VITE_DEV_BYPASS_ROLE=CLAIMANT
   ```
   Options: `CLAIMANT`, `CLAIMS_OFFICER`, `AUDITOR`, `ADMIN`.
2. Stop the dev server (`Ctrl + C`).
3. Start it again (`npm run dev`).
4. Hard-refresh the browser (`Ctrl + Shift + R`).

| Role | What you will see |
|---|---|
| `CLAIMANT` | Own claims, upload, no review, no audit, no analyse |
| `CLAIMS_OFFICER` | Full claims workspace, analyse button, review form, findings resolve |
| `AUDITOR` | Read-only runs, summaries, audit log |
| `ADMIN` | Audit log and About/versions |

The `hasRole()` implementation treats `ADMIN` as satisfying all role checks — this matches the
backend's authorization model.

---

## Environment variables

All variables are **public** (bundled into the client). **Never put secrets here.**

| Name | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Backend API base URL |
| `VITE_ENTRA_CLIENT_ID` | — | Entra ID application (client) ID |
| `VITE_ENTRA_TENANT_ID` | — | Entra ID directory (tenant) ID |
| `VITE_ENTRA_AUTHORITY` | `https://login.microsoftonline.com/common` | MSAL authority URL |
| `VITE_ENTRA_REDIRECT_URI` | `http://localhost:5173` | Redirect URI registered in Entra |
| `VITE_ENTRA_API_SCOPE` | — | API scope, e.g. `api://mmaug-claims-api/access_as_user` |
| `VITE_ENV_NAME` | `Local` | Environment label shown in the UI (`Local`, `Dev`, `Test`) |
| `VITE_APP_VERSION` | `0.0.0` | Client version label |
| `VITE_DEV_BYPASS_AUTH` | (unset) | **Development only.** `true` to enable auth bypass |
| `VITE_DEV_BYPASS_ROLE` | `CLAIMS_OFFICER` | **Development only.** Role to impersonate when the bypass is enabled |

Copy `.env.example` to `.env` to start. `.env` is gitignored; `.env.example` is tracked.

---

## Scripts

| Script | Purpose |
|---|---|
| `npm run dev` | Start the Vite dev server |
| `npm run build` | Type-check (`tsc -b`) and build for production |
| `npm run preview` | Preview the production build |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run lint` | ESLint |
| `npm test` | Run Vitest once |
| `npm run test:watch` | Vitest in watch mode |

---

## Application architecture

```
┌─────────────────────────────────────────────────────────────────┐
│ main.tsx                                                        │
│  ├─ msalInstance.initialize()                                   │
│  ├─ safeHandleRedirect()      ← no-op when bypass is enabled    │
│  ├─ setTokenProvider(acquireApiToken)                           │
│  └─ ReactDOM.render(<AppProviders><App /></AppProviders>)       │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ AppProviders                                                    │
│  ├─ MsalProvider           (Entra ID)                           │
│  ├─ QueryClientProvider    (TanStack Query)                     │
│  ├─ ThemeProvider          (light/dark, follows OS by default)  │
│  └─ BrowserRouter          (public + authenticated routes)      │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ AppRoutes                                                       │
│  ├─ Public         / /features /how-it-works /security ...      │
│  └─ Authenticated  /app  → <RoleGuard><AppLayout /></RoleGuard> │
└─────────────────────────────────────────────────────────────────┘
```

**Separation of concerns:**

- **`src/lib/api-client.ts`** — HTTP client. MSAL-agnostic. Receives a token provider via
  `setTokenProvider`.
- **`src/lib/auth.ts`** — MSAL instance, login request, token acquisition, bypass injection. The
  single place that wires MSAL to the API client.
- **`src/features/auth/use-auth.ts`** — React hook that reads accounts from the MSAL instance
  and re-renders on auth events.
- **`src/services/*`** — typed wrappers around backend endpoints. No state.
- **TanStack Query** — all server state, caching, mutations, invalidation.
- **Local `useState`/`useReducer`** — UI-only state.

The API client never imports MSAL; `main.tsx` is the only file that connects them.

---

## Route map

### Public routes (no authentication required)

| Route | Component | Purpose |
|---|---|---|
| `/` | `HomePage` | Marketing landing page |
| `/features` | `FeaturesPage` | Detailed capability list |
| `/how-it-works` | `HowItWorksPage` | Pipeline walkthrough |
| `/security` | `SecurityPage` | Security and privacy controls |
| `/responsible-ai` | `ResponsibleAIPage` | AI boundaries and statement types |
| `/faq` | `FAQPage` | Frequently asked questions |
| `/login` | `LoginPage` | Microsoft sign-in |
| `/403` | `ForbiddenPage` | Public forbidden page |
| `*` | `NotFoundPage` | Public 404 |

### Authenticated routes (under `/app`, wrapped in `RoleGuard` and `AppLayout`)

| Route | Component | Required roles |
|---|---|---|
| `/app` | `DashboardPage` | any authenticated |
| `/app/claims` | `ClaimsListPage` | any authenticated |
| `/app/claims/new` | `NewClaimPage` | `CLAIMANT`, `CLAIMS_OFFICER` |
| `/app/claims/:id` | `ClaimDetailPage` | owner or `CLAIMS_OFFICER` |
| `/app/claims/:id/:tab` | `ClaimDetailPage` (tab) | same as above |
| `/app/runs/:runId` | `AgentRunPage` | `CLAIMS_OFFICER`, `AUDITOR` |
| `/app/audit` | `AuditLogPage` | `AUDITOR`, `ADMIN` |
| `/app/about` | `AboutPage` | `ADMIN`, `CLAIMS_OFFICER` |
| `/app/403` | `ForbiddenPage` | any authenticated |
| `/app/*` | `AppNotFoundPage` | any authenticated |

The claim detail page exposes six deep-linked tabs:

| Tab | Path | Notes |
|---|---|---|
| Overview | `/app/claims/:id/overview` | Summary card, doc counts, recent findings |
| Documents | `/app/claims/:id/documents` | Upload, extraction drawer, download, delete |
| Findings | `/app/claims/:id/findings` | Filter, resolve (officers only) |
| Summary | `/app/claims/:id/summary` | AI summary, evidence chips, version picker |
| Review | `/app/claims/:id/review` | Officers only |
| Activity | `/app/claims/:id/activity` | Unified timeline |

---

## Roles and UI behaviour

Roles are read from the `roles` claim of the ID token and are used **only** to shape navigation
and available actions. The backend enforces every permission independently.

| Role | UI capabilities |
|---|---|
| `CLAIMANT` | Create claims, upload documents, view own claims and summaries (read-only) |
| `CLAIMS_OFFICER` | View all claims, run analysis, view summaries, findings, runs, submit reviews |
| `AUDITOR` | Read-only access to runs, summaries, audit log |
| `ADMIN` | Audit log and system information; treated as satisfying all role checks in the UI |
| `SERVICE_AGENT` | Machine identity only — never used by the browser |

Hiding a button is not authorisation. Every mutation is re-checked by the backend.

---

## Feature reference

### Dashboard

`src/features/dashboard/dashboard-page.tsx`

- Role-aware stat cards (Ready for review, Escalated, Recently updated, Total visible)
- Recent claims list (up to 8)
- Officer-only review queue and escalations panels
- Claimant-only status overview
- Loading skeletons, empty states, error states with retry

Data sources: `/claims` with filter and sort parameters.

### Claims list

`src/features/claims/claims-list-page.tsx`

- Server-driven filtering, sorting, pagination
- Filters: claim number, policy number, claimant name, status, risk flag, date range
- All filter state stored in URL query params — deep-linkable and refresh-safe
- Row click navigates to claim detail
- Loading skeletons, empty results, error and retry states
- Responsive table with horizontal scroll on mobile

### New claim

`src/features/claims/new-claim-page.tsx`

- React Hook Form + Zod validation
- Fields: policy number, claimant name, phone (E.164), email, vehicle registration, incident
  date, incident time (optional), incident location, incident description, third-party flag,
  police report number, injuries flag, estimated amount (EUR)
- File dropzone: files queued at creation, uploaded after the claim is created
- Inline field errors, submission progress, cancel confirmation
- Backend validation errors mapped back to fields

Validation rules (mirrored from the backend):

| Field | Rule |
|---|---|
| `policyNumber` | Required, `POL-MT-######` |
| `claimantPhone` | Required, E.164 |
| `claimantEmail` | Required, valid email |
| `incidentDate` | Required, not in the future |
| `incidentDescription` | Required, 20–5000 characters |
| `estimatedAmount` | Optional, ≥ 0, at most two decimals |
| Files | PDF / JPG / JPEG / PNG / TIFF, ≤ 15 MB each, ≤ 20 per claim |

### Document upload & extraction

- `FileDropzone` (`src/features/documents/file-dropzone.tsx`) — drag-and-drop, file picker,
  per-file document-type select, size/type pre-check, per-file remove
- Client-side validation mirrors the backend, but the backend remains authoritative
- `ExtractionDrawer` shows extracted fields with confidence, page, and snippet
- Documents table shows scan status, processing status, page count, and per-row actions

Previewed types: PDF, JPG, JPEG, PNG, TIFF. No arbitrary HTML is rendered from uploads.

### Claim detail (tabs)

`src/features/claims/claim-detail-page.tsx`

Header card: claim number, status badge, policy (stated), vehicle, incident date, channel,
estimated amount, risk flag, created timestamp.

Tab content:

| Tab | Highlights |
|---|---|
| Overview | Incident detail, doc count, open finding count, recent runs |
| Documents | Upload, extraction drawer, download, delete (admins) |
| Findings | Filters (severity, type, resolution), resolve with note |
| Summary | Sections with typed statements, evidence chips, version picker |
| Review | Outcome select, conditional fields, submit |
| Activity | Timeline of documents, runs, and reviews |

### AI Claim Summary

Rendered in the **Summary** tab of a claim.

Statement types (each rendered with its own badge and colour):

| Type | Meaning | Colour |
|---|---|---|
| `RETRIEVED_FACT` | Returned by a read-only tool | Info (blue) |
| `EXTRACTED_FACT` | Extracted from a document, with page reference | Success (green) |
| `AI_OBSERVATION` | Neutral observation, always linked to evidence | Muted (grey) |
| `UNCERTAIN` | Could not be confirmed; explicitly flagged | Warning (amber) |
| `RECOMMENDED_CHECK` | Suggested officer action — never a decision | Default (primary) |

A legend is displayed above the sections. Each statement shows evidence chips; clicking a chip
opens a drawer showing the document name, page, and excerpt, or the tool name and call ID, or
the rule ID, or the originating claim form field.

Confidence is shown when supplied by the backend. Escalation recommendations are displayed as
advisory only. Older summary versions are selectable and read-only.

### Human review

`src/features/claims/tabs/review-tab.tsx`

Only available to `CLAIMS_OFFICER`. The five permitted outcomes:

- `ACKNOWLEDGED`
- `INFO_REQUESTED` — displays a "information requested" textarea (required)
- `ESCALATED_TO_SENIOR` — displays an agreement checkbox
- `ACCEPTED_FOR_INVESTIGATION` — explicitly **not** approval of payment
- `SUMMARY_REJECTED_REGENERATE`

A comment is required for every outcome. Review history is listed below the form.

### Agent run details

`src/features/agent-runs/agent-run-page.tsx`

- Run meta: status, escalation flag, prompt/tool/rule versions, LLM model, token counts
- Vertical step timeline; each step expands to show input/output summaries and error code
- Tool call cards: tool name, status, HTTP status, latency, retries, arguments, result JSON
- Warnings and error banners
- Supported statuses: `QUEUED`, `RUNNING`, `COMPLETED`, `COMPLETED_WITH_WARNINGS`, `ESCALATED`,
  `FAILED`
- Supported step types: `SCAN_CHECK`, `OCR`, `STRUCTURE`, `CONSOLIDATE`, `VALIDATE`,
  `TOOL_CALL`, `GUARDRAIL`, `GENERATE_SUMMARY`, `VERIFY_EVIDENCE`, `ESCALATION_CHECK`
- Tool statuses: `SUCCESS`, `NOT_FOUND`, `FAILED`, `TIMEOUT`, `DENIED`, `CIRCUIT_OPEN`

A failed backend run is never displayed as completed.

### Audit log

`src/features/audit/audit-log-page.tsx`

Only available to `AUDITOR` and `ADMIN`. Columns: time, actor, role, action, outcome, entity,
correlation ID. Filters by claim, actor, action, outcome, date range, correlation ID. Pagination
(30 per page). Detail drawer shows redacted metadata, hash, and prev-hash. Audit events are
insert-only from the UI's perspective — no edit or delete actions exist.

### System information

`src/features/system/about-page.tsx`

Shows the current session (name, username, roles) and backend versions (app version, git commit,
prompt version, tool schema version, rule set version, rule set SHA-256). If the version
endpoint is unavailable, an explicit "unavailable" state is shown rather than a fabricated one.

---

## Backend integration

The API client (`src/lib/api-client.ts`) provides:

- Configurable base URL via `VITE_API_BASE_URL`
- JSON and `multipart/form-data` request bodies
- Bearer token injection via a pluggable token provider
- `AbortSignal` support for request cancellation
- Timeouts via `AbortController`
- Normalised error type `ApiRequestError` that preserves `status`, `code`, `correlationId`, and
  `fieldErrors` from the backend's `ApiError` shape

Service modules under `src/services/*` map 1:1 to backend endpoints. Types under `src/types/*`
mirror the backend DTOs.

**Endpoint summary** (see the RFP file map for the full contract):

| Area | Service | Key endpoints |
|---|---|---|
| User | `system.ts` | `GET /me` |
| Claims | `claims.ts` | `GET/POST /claims`, `GET/PATCH /claims/:id`, `POST /claims/:id/submit`, `POST /claims/:id/analyse` |
| Documents | `documents.ts` | `GET/POST /claims/:id/documents`, `DELETE /documents/:id`, `GET /documents/:id/download` |
| Extraction | `extraction.ts` | `GET /documents/:id/extraction` |
| Findings | `findings.ts` | `GET /claims/:id/findings`, `PATCH /findings/:id/resolve` |
| Summaries | `summaries.ts` | `GET /claims/:id/summary`, `GET /claims/:id/summary/versions` |
| Reviews | `reviews.ts` | `GET/POST /claims/:id/reviews` |
| Agent runs | `agent-runs.ts` | `GET /claims/:id/agent-runs`, `GET /agent-runs/:runId` |
| Audit | `audit.ts` | `GET /audit-events` |
| Versions | `system.ts` | `GET /admin/versions` |

If a backend endpoint is unavailable, its UI surfaces an error and a retry action. Mock data is
never used in production code paths.

---

## Responsible AI rules

The UI must never present:

- Approval or rejection of a claim
- Determination of final liability
- Authorisation of payment
- Modification of a policy

The only permitted review outcomes are:

```text
ACKNOWLEDGED
INFO_REQUESTED
ESCALATED_TO_SENIOR
ACCEPTED_FOR_INVESTIGATION
SUMMARY_REJECTED_REGENERATE
```

Statement types are visually distinguished so reviewers always know whether they are looking at
a retrieved fact, an extracted fact, an AI observation, an uncertain item, or a recommended
check. See [AI Claim Summary](#ai-claim-summary).

Accepting a claim for investigation is explicitly not approval of payment. This distinction is
stated in the review form and on the public Responsible AI page.

---

## Testing

```bash
npm test           # single run
npm run test:watch # watch mode
```

Coverage areas:

| Test file | Scope |
|---|---|
| `fileRules.test.ts` | File type, size, and count rules |
| `validators.test.ts` | Zod schemas for claim creation |
| `review-schema.test.ts` | Zod schema for review submission |
| `ClaimForm.test.tsx` | Form validation and error display |
| `StatementItem.test.tsx` | Statement rendering, type tags, evidence chips |
| `RoleGuard.test.tsx` | Authentication and role gating |
| `EvidenceDrawer.test.tsx` | Document and tool evidence rendering |

API calls are mocked at the service boundary. Production code never depends on mocks.

Tests run in jsdom with `@testing-library/react`. Run `npm run typecheck` before committing.

---

## Production build & deployment

```bash
npm run build
npm run preview
```

Deploy `dist/` to any static host: Azure Static Web Apps, Azure CDN, Nginx, or a container.

**Before shipping to any deployed environment:**

- [ ] `VITE_DEV_BYPASS_AUTH` is unset or `false`
- [ ] `VITE_ENTRA_*` variables point at the production tenant and app registration
- [ ] The production redirect URI is registered in Entra and matches `VITE_ENTRA_REDIRECT_URI`
- [ ] `VITE_ENV_NAME` reflects the target environment
- [ ] Content Security Policy (if applied at the host) allows `login.microsoftonline.com`
- [ ] The backend's CORS allow-list includes the frontend origin

---

## Security notes

- All Entra ID credential handling is done by Microsoft. The app never collects passwords.
- Access tokens are acquired silently and attached by the API client only to same-origin API
  requests. Tokens are never logged.
- Untrusted document content is never rendered as HTML. Document previews use safe rendering
  paths only.
- No secrets are stored in source or in `VITE_`-prefixed variables — those are bundled into the
  client.
- Frontend role checks are cosmetic. The backend remains the authority for every permission.
- The dev auth bypass is compiled out of production builds and refuses to start when the flag is
  set in a production build.

---

## Troubleshooting

### `/app` redirects to `/login`

- Confirm the bypass is on: `VITE_DEV_BYPASS_AUTH=true` in `.env`.
- Restart the dev server. Vite reads `.env` only at startup.
- Hard-refresh the browser (`Ctrl + Shift + R`).
- Open DevTools and confirm the console shows:
  ```
  [auth] DEV BYPASS ENABLED — impersonating role CLAIMS_OFFICER.
  ```

### Console shows `getUserRoles` is missing from `@/lib/auth`

- Vite served a stale copy of `auth.ts`. Clear the cache:
  ```bash
  rm -rf node_modules/.vite
  npm run dev
  ```
- Hard-refresh with **Disable cache** enabled in DevTools' Network tab.

### `Maximum update depth exceeded`

- This is caused by a `useSyncExternalStore` `getSnapshot` that returns a new array each call.
  `use-auth.ts` avoids this by tracking accounts in `useState` and refreshing only on MSAL
  events. If the error returns, ensure `use-auth.ts` matches the current implementation in this
  repository.

### Stale MSAL state causes a redirect to Microsoft on load

- The app guards against this by skipping `handleRedirectPromise()` when the bypass is enabled.
- To clear any leftover state manually: DevTools → **Application** → **Storage** → **Clear site
  data** for `http://localhost:5173`. Also unregister any service workers listed.

### `304 Not Modified` on `/app`

- The dev server sets `Cache-Control: no-store` in `vite.config.ts`. If you still see 304,
  ensure `vite.config.ts` includes the `server.headers` block and restart the dev server.
- In DevTools → Network, tick **Disable cache** while developing.

### Sign-in from Entra ID redirects back to Microsoft in a loop

- Confirm the redirect URI registered in Entra exactly matches `VITE_ENTRA_REDIRECT_URI`.
- Confirm the SPA app registration has the "Single-page application" platform (not "Web").
- Confirm `VITE_ENTRA_API_SCOPE` matches a scope the API app exposes.

### Backend calls return 401 with the bypass enabled

- Expected. The bypass issues no token. Run against a backend with security disabled in local
  development, or configure real Entra ID.

---

## Known limitations

- Tailwind pinned to 3.4 (see [Stack](#stack)).
- Recharts is installed but chart panels are intentionally minimal — metrics only render when
  the backend supplies real data.
- The development auth bypass is intended for local preview only. It must never be enabled in
  shared, test, or production environments.
- No SSR. The public site is a client-rendered SPA. SEO basics are handled via meta tags and
  semantic HTML. Introducing SSR would change the deployment model and is out of scope for this
  MVP.
- Localisation: strings are English only.

---

## Directory structure

```text
src/
├── app/                          Router + providers + query client
│   ├── providers.tsx
│   ├── query-client.ts
│   └── router.tsx
├── components/
│   ├── layout/                   App shell
│   │   ├── app-layout.tsx
│   │   ├── app-sidebar.tsx
│   │   ├── app-topbar.tsx
│   │   ├── page-header.tsx
│   │   └── index.ts
│   ├── theme-provider.tsx
│   └── ui/                       shadcn/ui primitives
├── features/
│   ├── auth/                     MSAL wiring, login, role guard
│   ├── marketing/                Public site
│   ├── dashboard/                Role-aware overview
│   ├── claims/                   List, detail, tabs, status badges
│   │   └── tabs/
│   ├── documents/                FileDropzone + extraction drawer
│   ├── summary/                  Evidence chips, statement tags, drawers
│   ├── review/                   Review form + history
│   ├── agent-runs/               Run timeline + tool call cards
│   ├── audit/                    Audit log + detail drawer
│   └── system/                   About / versions
├── lib/
│   ├── api-client.ts             HTTP client (MSAL-agnostic)
│   ├── auth.ts                   MSAL setup + token provider
│   ├── env.ts                    Runtime env access
│   └── utils.ts                  cn() + formatting helpers
├── schemas/                      Zod schemas
├── services/                     Typed API wrappers
├── types/                        Shared TypeScript types
├── styles/
│   └── global.css                Tailwind + theme variables
├── tests/                        Vitest setup + helpers + specs
├── App.tsx
├── main.tsx
└── vite-env.d.ts
```

---

## License

Training simulation for the MMAUG AI & DevOps Bootcamp. No production use.