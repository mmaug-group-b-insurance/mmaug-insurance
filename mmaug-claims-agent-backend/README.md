# MMAUG Insurance Claims Agent — Backend

Spring Boot backend for the **MMAUG Insurance Claims Agent** (RFP `MMAUG-AIDB-RFP-003`).

The system provides **AI-assisted claims processing with human oversight and auditable workflows**.

> **Responsible AI:** This system is decision-support only. It never approves, rejects, determines liability, authorises payment, or modifies a policy. Human claims officers remain accountable.

---

## Table of Contents

1. [How It Works — Overview](#how-it-works--overview)
2. [How It Works — End-to-End Flow](#how-it-works--end-to-end-flow)
3. [How It Works — Agent Pipeline](#how-it-works--agent-pipeline)
4. [How It Works — Data Model](#how-it-works--data-model)
5. [How It Works — Security & Roles](#how-it-works--security--roles)
6. [How It Works — Responsible AI Controls](#how-it-works--responsible-ai-controls)
7. [Technology Stack](#technology-stack)
8. [Prerequisites](#prerequisites)
9. [Quick Start](#quick-start)
10. [Swagger UI & OpenAPI](#swagger-ui--openapi)
11. [Build & Test](#build--test)
12. [Docker](#docker)
13. [Database Migrations](#database-migrations)
14. [Seed Data](#seed-data)
15. [Configuration](#configuration)
16. [API Endpoints](#api-endpoints)
17. [Project Structure](#project-structure)
18. [Troubleshooting](#troubleshooting)
19. [License](#license)

---

## How It Works — Overview

The backend has **four layers**, each with a single clear responsibility:

```text
┌─────────────────────────────────────────────────────────────────────┐
│ 1. REST API Layer (controllers)                                     │
│    Validates requests, enforces RBAC, returns DTOs. Never returns   │
│    JPA entities. Never contains business logic.                     │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│ 2. Service Layer (business logic)                                   │
│    ClaimService, DocumentService, ExtractionService,                │
│    FindingService, ReviewService, AuditService,                     │
│    ClaimsAgentOrchestrator, DashboardService                        │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│ 3. Persistence Layer (JPA repositories + entities)                  │
│    All 22 entities mapped to 13 Flyway migrations.                  │
│    Optimistic locking via @Version. Auditing via @CreatedDate.      │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│ 4. Cross-cutting concerns                                           │
│    Security (OAuth2 + RBAC), Audit (hash chain),                    │
│    Guardrails (injection, forbidden wording, uncertainty),          │
│    Rate limiting, Correlation IDs, PII encryption, Retention        │
└─────────────────────────────────────────────────────────────────────┘
```

The system deliberately avoids microservices. Everything runs in one Spring Boot application with clear module boundaries.

---

## How It Works — End-to-End Flow

### 1. Claimant creates a claim

```text
React frontend
  │
  ├─► MSAL signs the user in against Entra ID
  │
  ├─► POST /api/v1/claims (with Bearer JWT)
  │
  ▼
Backend:
  ClaimService.create()
    ├─ Validates the request (Bean Validation)
    ├─ Generates CLM-2026-000123 via ClaimNumberGenerator
    ├─ Attempts to link to a Policy (best-effort)
    ├─ Saves Claim with status DRAFT
    └─ Records audit event CLAIM_CREATED
```

### 2. Claimant uploads documents

```text
POST /api/v1/claims/{id}/documents (multipart)
  │
  ▼
DocumentService.upload()
  ├─ FileValidator validates:
  │    - extension (pdf/jpg/jpeg/png/tiff)
  │    - magic bytes (content matches extension)
  │    - size ≤ 15 MB
  │    - count ≤ 20 per claim
  ├─ Computes SHA-256
  ├─ Stores file in quarantine container
  ├─ Persists ClaimDocument (scan_status=PENDING)
  ├─ Runs MalwareScanner (NoOp in dev, Defender in prod)
  ├─ Updates scan_status=CLEAN, processing_status=UPLOADED
  └─ Records audit event DOCUMENT_UPLOADED
```

### 3. Claimant submits the claim

```text
POST /api/v1/claims/{id}/submit
  │
  ▼
ClaimService.submit()
  ├─ Verifies owner or officer
  ├─ Requires status DRAFT
  ├─ Sets status=SUBMITTED, submitted_at=now
  └─ Records audit event CLAIM_SUBMITTED
```

### 4. Claims officer triggers analysis

```text
POST /api/v1/claims/{id}/analyse
  │
  ▼
ClaimsAgentOrchestrator.execute()
  └─ Runs the full agent pipeline (see next section)
```

### 5. Officer reviews findings and summary

```text
GET /api/v1/claims/{id}/findings
GET /api/v1/claims/{id}/summary
GET /api/v1/agent-runs/{runId}
  │
  ▼
Officer reads typed statements, evidence, and tool calls
```

### 6. Officer submits a human review

```text
POST /api/v1/claims/{id}/reviews
  │
  ▼
ReviewService.create()
  ├─ Verifies CLAIMS_OFFICER role
  ├─ Requires infoRequested when outcome = INFO_REQUESTED
  ├─ Persists OfficerReview + optional OfficerEdits
  ├─ Updates Claim status per outcome
  └─ Records audit event REVIEW_SUBMITTED
```

**No APPROVED or REJECTED outcome exists.** The final claim decision is made outside this system.

---

## How It Works — Agent Pipeline

The orchestrator runs a **fixed 11-step pipeline**. The LLM is only invoked inside bounded steps. Every step writes an `agent_step` row.

| Step | Type | Description |
|---|---|---|
| 1 | `SCAN_CHECK` | Only documents with `scan_status=CLEAN` proceed |
| 2 | `OCR` | Document Intelligence returns text and page numbers |
| 3 | `STRUCTURE` | LLM fills the claim-facts schema from OCR text, treated as DATA only — never as instructions |
| 4 | `GUARDRAIL` | Heuristic + classifier check for prompt injection; flags, never obeys |
| 5 | `CONSOLIDATE` | Merge per-document fields into one `ClaimFact` per field, with provenance and confidence |
| 6 | `VALIDATE` | Rule engine: missing fields, contradictions, low-confidence flags → writes `ValidationFinding` rows |
| 7 | `TOOL_CALL` | Read-only tools: `get_policy`, `get_coverage`, `get_claim_history`, `get_business_rules` |
| 8 | `VALIDATE` | Compare claim facts vs policy facts (dates, vehicle, driver, coverage) |
| 9 | `GENERATE_SUMMARY` | LLM writes typed statements, each citing evidence |
| 10 | `VERIFY_EVIDENCE` | Drop or downgrade any statement without a valid source reference |
| 11 | `ESCALATION_CHECK` | Rules + uncertainty decide escalation recommendation |

### Tool allow-list

Only four tools exist, all GET-only, all read-only:

| Tool | Returns |
|---|---|
| `get_policy` | Policy status, dates, product, policyholder, vehicles |
| `get_coverage` | Coverage lines, limits, excesses, exclusions |
| `get_claim_history` | Prior claims within a configurable window |
| `get_business_rules` | Required fields, thresholds, escalation rules |

Tools are served by `/internal/tools/v1/**` — accessible **only to `SERVICE_AGENT`**.

### Agent run outcomes

| Status | Meaning |
|---|---|
| `COMPLETED` | All steps succeeded, no warnings, no escalation |
| `COMPLETED_WITH_WARNINGS` | Completed but a tool failed or data was partial |
| `ESCALATED` | Escalation rules triggered (injury, high value, injection, etc.) |
| `FAILED` | Unrecoverable error |

---

## How It Works — Data Model

The system has **22 entities**. The core relationships:

```text
Customer 1──────* Policy 1──────* Vehicle
                         │
                         ├──────* PolicyDriver
                         ├──────* PolicyCoverage
                         └──────* HistoricalClaim

AppUser 1──────* Claim (created_by)
AppUser 1──────* Claim (assigned_officer)

Claim 1──────* ClaimDocument 1──────* ExtractionResult 1──────* ExtractedField
Claim 1──────* ClaimFact
Claim 1──────* ValidationFinding
Claim 1──────* AgentRun 1──────* AgentStep
                    │
                    └──────* ToolCall
Claim 1──────* ClaimSummary 1──────* SummaryStatement 1──────* EvidenceRef
Claim 1──────* OfficerReview 1──────* OfficerEdit

AuditEvent       (insert-only, hash-chained, no @Version)
RetentionPolicy  (per-entity retention rules)
```

### Invariants enforced

- No `APPROVED`, `REJECTED`, or `PAID` value exists in any enum.
- Every summary statement of type `RETRIEVED_FACT` or `EXTRACTED_FACT` must resolve to a real `EvidenceRef`.
- `audit_event` has `REVOKE UPDATE, DELETE` at the database level.
- All list queries use `@Version` for optimistic locking.
- PII columns are encrypted at the application layer via `AttributeEncryptor` (AES-GCM).

---

## How It Works — Security & Roles

### Authentication flow

```text
React frontend ──MSAL──► Microsoft Entra ID
                              │
                              ▼ JWT
React frontend ────────► Backend
                              │
                              ├─ JwtDecoder validates signature + issuer
                              ├─ JwtAuthConverter maps "roles" claim → ROLE_*
                              ├─ CurrentUserResolver injects CurrentUser
                              └─ Authorization enforced per endpoint
```

### Role matrix

| Resource | CLAIMANT | OFFICER | AUDITOR | ADMIN | SERVICE_AGENT |
|---|---|---|---|---|---|
| Create claim | Yes | Yes | No | No | No |
| Read own claim | Yes | Yes | No | No | No |
| Read any claim | No | Yes | Yes | Yes | No |
| Trigger analyse | No | Yes | No | No | No |
| View summary | No | Yes | Yes | Yes | No |
| Submit review | No | Yes | No | No | No |
| Resolve finding | No | Yes | No | No | No |
| View audit | No | No | Yes | Yes | No |
| Internal tools | No | No | No | No | Yes |
| Dashboard | Own | All | All | All | No |

### Defence-in-depth controls

| Layer | Control |
|---|---|
| Network | HTTPS, HSTS, CORS allow-list |
| AuthN | OAuth2 resource server; JWT signature + issuer + audience |
| AuthZ | Route rules + `@PreAuthorize` + `ClaimAccessPolicy` |
| Input | Bean Validation, strict Jackson deserialisation |
| File | Magic-byte sniffing, extension allow-list, size + count limits, quarantine |
| Prompt | Documents wrapped in `<DATA>` blocks; `PromptInjectionDetector` |
| Output | `OutputSchemaValidator`, `EvidenceVerifier`, `ForbiddenClaimsFilter` |
| Tool | Allow-list, GET-only, schema-validated args, timeouts, circuit breaker |
| Data | `AttributeEncryptor` for PII, `PiiMasker` in logs |
| Audit | Insert-only table, SHA-256 hash chain, DB-level REVOKE |
| Secrets | Environment variables; nothing in source control |

---

## How It Works — Responsible AI Controls

The RFP requires that the AI never autonomously approve, reject, determine liability, authorise payment, or modify policy. This is enforced **structurally**, not by convention:

| Control | Mechanism | File |
|---|---|---|
| No approve/reject enum | Type-level absence | `ReviewOutcome`, `ClaimStatus` |
| Tools are read-only | Allow-list of GET-only tools | `AgentToolRegistry` |
| Forbidden wording blocked | Phrase filter on AI output | `ForbiddenClaimsFilter` |
| Prompt injection flagged | Regex heuristics | `PromptInjectionDetector` |
| Evidence traceable | Every statement has `EvidenceRef` | `EvidenceVerifier` |
| Uncertainty surfaced | Explicit `UNCERTAIN` statement type | `UncertaintyPolicy` |
| Architecture boundary | ArchUnit test fails build on violation | `ArchitectureTest` |
| Human decision | Reviews are acknowledgement/escalation only | `ReviewService` |
| Immutable log | DB-level REVOKE + hash chain | `AuditService`, `V11` |

If any future change tries to add an approval path, at least one of the above controls will fail loudly.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (verified on 25) |
| Framework | Spring Boot 4.1.x |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + PostgreSQL + Flyway |
| Security | Spring Security + OAuth2 Resource Server (Entra ID) |
| API Docs | springdoc-openapi (Swagger UI + OpenAPI 3) |
| Resilience | Resilience4j |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, ArchUnit |
| Build | Maven |
| Container | Docker + Docker Compose |

---

## Prerequisites

- **Java 21 or 25**
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 14+** (container provided)
- **Microsoft Entra ID tenant** (production only)

---

## Quick Start

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env — set ENTRA_ISSUER_URI if you have a tenant
```

### 3. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Verify

```bash
curl http://localhost:8080/actuator/health/liveness
```

---

## Swagger UI & OpenAPI

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

### How to authenticate through Swagger UI

1. Open Swagger UI.
2. Click **Authorize**.
3. Enter `Bearer <your-JWT-token>`.
4. Click **Authorize**.

Swagger is enabled by default in `local`/`dev`/`test` and disabled in `prod`.

---

## Build & Test

```bash
mvn -B dependency:resolve
mvn clean compile
mvn clean verify
mvn clean package
java -jar target/mmaug-claims-agent-backend-*.jar
```

### Test coverage

- Boundary tests (no approve/reject/paid enums)
- Guardrail tests (forbidden wording, injection)
- File validation tests
- Architecture test (ArchUnit)

---

## Docker

```bash
docker compose up --build
docker compose down
docker compose down -v
```

### Health check

```bash
curl http://localhost:8080/actuator/health/liveness
```

---

## Database Migrations

Location: `src/main/resources/db/migration/`

| Migration | Purpose |
|---|---|
| V1 | Users and customers |
| V2 | Policies, vehicles, drivers, coverages |
| V3 | Historical claims |
| V4 | Claims and facts |
| V5 | Claim documents |
| V6 | Extraction tables |
| V7 | Agent runs, steps, tool calls |
| V8 | Summaries, statements, evidence |
| V9 | Validation findings |
| V10 | Officer reviews |
| V11 | Audit events (hash-chained) |
| V12 | Retention policies |
| V13 | Composite indexes and constraints |

### Checking migration state

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

### Recovering from a failed migration (local only)

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

> **Do not edit a migration file after it has been applied.**

---

## Seed Data

Location: `src/main/resources/db/seed/` (loaded only in `local`/`dev`/`test`).

| File | Contents |
|---|---|
| `R__seed_customers.sql` | 5 synthetic customers |
| `R__seed_policies.sql` | 5 policies (ACTIVE, LAPSED, EXPIRED) |
| `R__seed_vehicles_drivers.sql` | Vehicles and drivers |
| `R__seed_coverages.sql` | Coverage lines |
| `R__seed_historical_claims.sql` | Prior claims |
| `R__seed_sample_claims.sql` | 3 sample claims |

All seed data is obviously synthetic.

---

## Configuration

See `.env.example`.

### Key properties

| Property | Description |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `ENTRA_ISSUER_URI` | Microsoft Entra ID issuer URL |
| `ENTRA_AUDIENCE` | Expected JWT audience |
| `STORAGE_PATH` | Local file storage path (dev) |
| `PII_ENCRYPTION_KEY` | AES-GCM key for PII columns (32+ bytes) |
| `AI_ENDPOINT`, `AI_DEPLOYMENT` | Azure OpenAI (optional) |
| `DOCINTEL_ENDPOINT` | Azure Document Intelligence (optional) |
| `APP_VERSION`, `PROMPT_VERSION`, `TOOL_VERSION`, `RULES_VERSION` | Version metadata |

### Profiles

| Profile | Purpose |
|---|---|
| `local` | Local dev, seed enabled |
| `dev` | Shared dev environment |
| `test` | Testcontainers |
| `prod` | Production, Swagger disabled |

---

## API Endpoints

Base path: `/api/v1`

| Resource | Method | Path | Role |
|---|---|---|---|
| Auth | GET | `/me` | Any |
| Claims | POST | `/claims` | CLAIMANT, OFFICER |
| Claims | GET | `/claims` | Owner / OFFICER |
| Claims | GET | `/claims/{id}` | Owner / OFFICER |
| Claims | PATCH | `/claims/{id}` | Owner (draft) / OFFICER |
| Claims | POST | `/claims/{id}/submit` | Owner |
| Documents | POST | `/claims/{id}/documents` | Owner / OFFICER |
| Documents | GET | `/claims/{id}/documents` | Owner / OFFICER |
| Documents | GET | `/documents/{id}/download` | Owner / OFFICER |
| Documents | DELETE | `/documents/{id}` | Owner (draft) / ADMIN |
| Extraction | GET | `/documents/{id}/extraction` | OFFICER |
| Extraction | POST | `/documents/{id}/extraction` | OFFICER |
| Findings | GET | `/claims/{id}/findings` | OFFICER |
| Findings | PATCH | `/findings/{id}/resolve` | OFFICER |
| Agent | POST | `/claims/{id}/analyse` | OFFICER |
| Agent | GET | `/claims/{id}/agent-runs` | OFFICER, AUDITOR |
| Agent | GET | `/agent-runs/{runId}` | OFFICER, AUDITOR |
| Summary | GET | `/claims/{id}/summary` | OFFICER |
| Review | POST | `/claims/{id}/reviews` | OFFICER |
| Review | GET | `/claims/{id}/reviews` | OFFICER, AUDITOR |
| Audit | GET | `/audit-events` | AUDITOR, ADMIN |
| Dashboard | GET | `/dashboard` | Any |
| System | GET | `/admin/versions` | ADMIN, OFFICER |
| Internal | GET | `/internal/tools/v1/**` | SERVICE_AGENT |
| Health | GET | `/actuator/health/liveness` | Public |

---

## Project Structure

```text
src/main/java/com/mmaug/claimsagent/
├── MmaugClaimsAgentBackendApplication.java
├── config/
├── security/
├── common/
├── user/
├── customer/
├── policy/
├── claim/
├── document/
├── extraction/
├── validation/
├── rules/
├── agent/
│   ├── prompt/
│   ├── tools/
│   ├── guardrails/
│   └── summary/
├── tools/
├── review/
├── audit/
├── retention/
├── dashboard/
└── admin/
```

---

## Troubleshooting

### Flyway checksum mismatch

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Then restart. Do not edit migrations after applying them.

### Empty migration placeholders

Delete all 0-byte files in `db/migration/`. Only create a migration when you have its full SQL ready.

### Hibernate schema validation error

Match Hibernate's default types:

| Java type | SQL type |
|---|---|
| `String` with `@Column(length = N)` | `VARCHAR(N)` |
| `String` with `columnDefinition = "TEXT"` | `TEXT` |
| `UUID` | `UUID` |
| `Instant` | `TIMESTAMPTZ` |
| `BigDecimal(p, s)` | `NUMERIC(p, s)` |

### Protected endpoints return 401 in local

Configure Entra ID and pass a token, or test only public endpoints.

---

## License

Proprietary — MMAUG Insurance Ltd.