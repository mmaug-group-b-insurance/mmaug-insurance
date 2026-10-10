# MMAUG Insurance Claims Agent — Backend

Spring Boot backend for the **MMAUG Insurance Claims Agent** (RFP `MMAUG-AIDB-RFP-003`).

The system provides **AI-assisted claims processing with human oversight and auditable workflows**.

> **Responsible AI:** This system is decision-support only. It never approves, rejects, determines liability, authorises payment, or modifies a policy. Human claims officers remain accountable.

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA + PostgreSQL + Flyway |
| Security | Spring Security + OAuth2 Resource Server (Microsoft Entra ID) |
| API Docs | springdoc-openapi 2.8.17 (Swagger UI + OpenAPI 3) |
| Resilience | Resilience4j |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, ArchUnit |
| Build | Maven |

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for local PostgreSQL)
- A Microsoft Entra ID tenant (for production authentication)

## Quick Start (Local)

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env — set ENTRA_ISSUER_URI if you have a tenant
```

For local development without Entra ID, the application starts but protected endpoints return 401 until a valid JWT is provided.

### 3. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Verify

```bash
curl http://localhost:8080/actuator/health/liveness
```

## Swagger UI & OpenAPI

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

### How to authenticate through Swagger UI

1. Open the Swagger UI URL.
2. Click the **Authorize** button (top right).
3. In the **Value** field, enter: `Bearer <your-JWT-token>`
4. Click **Authorize**.

All subsequent "Try it out" requests will include `Authorization: Bearer <token>`.

The Authorize button is configured via `OpenApiConfig` using the `bearerAuth` security scheme.

Swagger is enabled by default in `local`/`dev`/`test` profiles and disabled in `prod` (configurable via `SPRINGDOC_API_DOCS_ENABLED` / `SPRINGDOC_SWAGGER_UI_ENABLED`).

## Build & Test

```bash
# Install dependencies
mvn -B dependency:resolve

# Run unit + integration tests
mvn clean verify

# Build the JAR
mvn clean package

# Run the built JAR
java -jar target/mmaug-claims-agent-backend-*.jar
```

## Docker

```bash
# Build and start the full stack (Postgres + backend)
docker compose up --build

# Stop
docker compose down

# Stop and remove volumes
docker compose down -v
```

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`.

- `V1__create_users_customers.sql` — user and customer tables
- Subsequent migrations added per implementation phase.

Migrations run automatically at application startup.

## Configuration

See `.env.example` for all supported environment variables.

Key properties:

| Property | Description |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `ENTRA_ISSUER_URI` | Microsoft Entra ID issuer |
| `ENTRA_AUDIENCE` | Expected JWT audience |
| `STORAGE_PATH` | Local file storage path (dev) |
| `PII_ENCRYPTION_KEY` | AES key for PII columns (32+ bytes) |
| `APP_VERSION`, `PROMPT_VERSION`, `TOOL_VERSION`, `RULES_VERSION` | Version metadata |

## Project Structure

```text
src/main/java/com/mmaug/claimsagent/
├── config/       — Configuration properties and beans
├── security/     — OAuth2, JWT converter, access policy, filters
├── common/       — BaseEntity, exceptions, utilities, crypto
├── user/         — AppUser (later phases)
├── customer/     — Customer (later phases)
├── policy/       — Policy, Vehicle, Driver, Coverage (later phases)
├── claim/        — Claim, ClaimFact (later phases)
├── document/     — Upload, validation, storage (later phases)
├── extraction/   — OCR + structuring (later phases)
├── validation/   — Findings (later phases)
├── rules/        — Business rules (later phases)
├── agent/        — Orchestrator, tools, guardrails, summary (later phases)
├── tools/        — Internal REST API for SERVICE_AGENT (later phases)
├── review/       — Officer reviews (later phases)
├── audit/        — Immutable audit log (later phases)
└── retention/    — Retention policies (later phases)
```

## Implementation Phases

| Phase | Deliverable | Status |
|---|---|---|
| 1 | Project setup (this phase) | ✅ |
| 2 | Core domain (users, customers, policies, claims) | ✅ |
| 3 | Documents (upload, validation, storage) | ✅ |
| 4 | Extraction (OCR, structuring, consolidation) | ✅ |
| 5 | Agent (runs, steps, tools, guardrails) | ✅ |
| 6 | Findings + summaries | ⏳ |
| 7 | Human review | ⏳ |
| 8 | Audit + retention | ⏳ |
| 9 | Dashboard + frontend support | ⏳ |
| 10 | Testing, Docker, OpenAPI, hardening | ⏳ |

## License

Proprietary — MMAUG Insurance Ltd.