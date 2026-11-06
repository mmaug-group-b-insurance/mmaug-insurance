# Java backend build baseline

Issue: [EN-02 #26](https://github.com/mmaug-group-b-insurance/mmaug-insurance/issues/26).
Inspection: 28 October 2026, based on main `da26eeb`.

The existing Java/Spring Boot backend remains in `mmaug-claims-agent-backend/`.
Use a full JDK 17, matching `pom.xml`, rather than the Java 8 JRE previously
selected on this PC. Maven is supplied by the checked-in wrapper.

Latest verification on 30 October 2026: **18 tests passed, no failures, errors or
skips**, using the isolated PostgreSQL test profile described below. Earlier
failure results in this document are retained as history.

## Windows checks

From the repository root, substitute the full path to your JDK directory:

```powershell
.\scripts\Invoke-BackendChecks.ps1 -JdkHome 'C:\path\to\jdk-17' -Goal compile
.\scripts\Invoke-BackendChecks.ps1 -JdkHome 'C:\path\to\jdk-17' -Goal test
```

The script validates that a compiler is present and Java is at least 17. It selects
that JDK for the invoked commands and restores the caller's process environment
and directory. It does not change Windows user/system environment settings,
download a runtime, disable tests or start the application. Maven may download
dependencies into the user's Maven cache. Inspect failed test reports in
`mmaug-claims-agent-backend/target/surefire-reports/`.

The `test` goal also requires a running Docker-compatible engine (for example,
Docker Desktop using Linux containers) and access to the `postgres:16-alpine`
and Testcontainers cleanup images. The first run may download these images.
Missing Docker is a test failure, not a reason to skip the context test.

The context test explicitly selects `ClaimsAgentApplication` and profile `test`.
Testcontainers starts a disposable PostgreSQL 16 database using random host ports;
dynamic test properties override ambient datasource URL, driver and credentials.
Flyway runs only `db/migration`, not demo seed scripts. Uploaded evidence storage
uses a JUnit temporary directory. No normal local/shared database, password or
production configuration needs to be changed.

The real Spring Security filter chain remains enabled. Only the JWT decoder is
mocked in this test context to avoid external Entra calls; anonymous claims access
is still checked and returns 401. This is not a valid-token/RBAC integration test.

Equivalent manual setup in a temporary PowerShell terminal:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location mmaug-claims-agent-backend
.\mvnw.cmd -v
.\mvnw.cmd -B -ntp compile
.\mvnw.cmd -B -ntp test
```

For JDK downloads use the official [Temurin archive instructions](https://adoptium.net/installation/archives/)
and verify the published SHA-256. Prefer the team's agreed JDK; do not migrate
Java/Spring Boot versions as part of environment setup.

## Verified results

Temurin JDK 17.0.20.1 and Maven wrapper 3.9.16 compiled the existing main source.
Jackson-related deprecation warnings remain; they did not fail compilation.

Initially test compilation failed because `ArchitectureTest` imports ArchUnit but
the POM did not declare it. This branch adds only the core ArchUnit 1.5.1 artifact
with test scope; runtime dependencies are unchanged.

After adding it, Surefire reported **16 tests, 1 failure, 1 error, 0 skipped**:

| Test | Result and follow-up |
| --- | --- |
| ReviewOutcomeBoundaryTest | Fails because SUMMARY_REJECTED_REGENERATE contains REJECT. Review with the backend/QA owners: rejecting an AI summary differs from adjudicating a claim. Preserve the forbidden claim/payment boundary; do not delete or skip the test simply to obtain green. |
| MmaugClaimsAgentBackendApplicationTests | Cannot choose between ClaimsAgentApplication and MmaugClaimsAgentBackendApplication, both marked SpringBootApplication. Agree a canonical application configuration with the backend maintainer and verify test/profile/database setup after resolving ambiguity. |

Fourteen other test cases passed. This does not establish complete AI coverage:
27 of 34 Java files under `src/test` were empty, and the prompt/schema files
inspected were empty. No cloud, live model, real OCR or end-to-end officer journey
has been validated by these checks. The suite remains red; issue #26 is not Done.

## Coordination

### Entry point update on 30 October 2026

The user selected ClaimsAgentApplication as canonical. The compatibility launcher
now delegates to it without a second SpringBootApplication annotation. The review
boundary test explicitly permits summary rejection/regeneration while restricting
outcomes to the agreed preparation actions. Latest full test run: **17 tests,
0 assertion failures, 1 context error, 0 skipped**. Spring selects the intended
configuration; context startup now reaches Flyway but PostgreSQL authentication
fails. Configure an isolated test database/profile with the backend maintainer
before claiming context or end-to-end success. No credentials were changed.

That PostgreSQL authentication result preceded test isolation. After activating
the test profile, adding the Spring Boot-managed Testcontainers PostgreSQL test
dependency and isolating resources, the full suite passed **18 tests**. The
context checks verify PostgreSQL, all 13 SQL migrations and denial of anonymous
claims access. No test was disabled or skipped to obtain this result. Placeholder
AI tests/prompts and unverified live integrations remain outside this coverage.

Frontend recordings/code were reviewed; the frontend remains unchanged. Local
officer bypass uses CLAIMS_OFFICER, and UI bypass supplies no API Bearer token.

The local feature branch `feature/ai-build-baseline` starts from inspected main.
Remote refs checked on 28 October show develop at `c2e911c` and main at `da26eeb`;
develop is an ancestor 26 commits behind main. No shared ref was changed. Agree
the integration base with the lead before pushing or opening a PR; do not merge
the entire branch into stale develop implicitly. The focused dependency/script/docs
change can be reapplied to the agreed base.

The next AI slice remains #30: labelled synthetic fixtures, versioned extraction
contracts and real consolidation tests. It should build on the existing Java DTOs
and services, coordinated with maintainers through #21, #22 and #23 decisions.
