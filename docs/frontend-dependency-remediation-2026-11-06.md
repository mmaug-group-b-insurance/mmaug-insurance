# Frontend dependency remediation — 6 November 2026

The project owner confirmed team acceptance of backend PR #83 and its integration into main. PR #83 was merged as `6e7a595dee42d710dd86dfa78df51b9f90c11e19` after the expected-head CI check passed. This document covers the separate frontend security increment based on that merge.

## Dependency changes

GitHub reported 16 open frontend alerts: 3 critical, 2 high and 11 moderate. npm audit counts affected packages differently; its original lockfile report contained 16 affected packages (2 critical, 2 high and 12 moderate). Neither count alone establishes exploitability of every advisory. The remediation updates affected code rather than dismissing alerts.

| Affected dependency | Resolved version / disposition |
| --- | --- |
| Vite | 6.4.5, keeping the existing Vite major |
| Vitest / @vitest/mocker | 4.1.12 |
| tinypool | Removed from the dependency graph by the Vitest upgrade |
| React Router / react-router-dom | 7.18.5, retaining the existing DOM-router API and React 18 |
| Zod | 4.6.7 |
| esbuild | 0.25.12 through the Vite dependency tree |
| @hookform/resolvers / react-hook-form | 5.2.2 / 7.82.0 for the Zod 4 input/output contract |
| TypeScript ESLint parser/plugin | 8.72.1 |
| minimatch used by TypeScript ESTree | Scoped override to 11.0.1; the upstream ^10.2.2 range remains affected by GHSA-g3hv-75wg-m249. No global override of ESLint's older minimatch consumers is applied. |
| postcss-selector-parser | 6.1.5 through a compatible lockfile refresh |

The scoped minimatch override is intentional and should be removed when TypeScript ESLint adopts a patched supported range. A real type-aware `parseForESLint` call against the claim schema and project tsconfig passed. This verifies the exercised parser path; it is not a full ESLint rules run.

## Compatibility and existing build gaps

The Zod/resolver upgrade distinguishes raw form inputs from normalized API values. The shared FormField helper now carries the transformed type; optional/blank estimates remain unknown and decimal text remains a number at submission. No validation rule or visual redesign is introduced.

Strict TypeScript verification also exposed pre-existing build gaps. The increment fills the previously empty extraction response type from the existing Spring Boot DTO, narrows run links by activity kind, removes unused imports/bindings and preserves nullable account typing. There are no Java endpoint changes or new claims decision capabilities.

Vitest now has an explicit jsdom/setup configuration. The test harness supplies ResizeObserver for Radix, gives redirect tests real destination routes, and checks the policy error rather than matching helper text. This prevents a guard redirect from looping inside a route-less test wrapper. Four schema regression cases cover unknown estimates and decimal normalization. Product routing, styling and layouts are preserved.

## Verification

Local Node 24.19.0 checks passed: 35 tests in seven files, strict TypeScript, production build and npm audit with zero vulnerabilities across production and development dependencies. The lockfile checksum and local results are in `frontend-dependency-test-results-2026-11-06.json`. Testing used an isolated checkout without the existing preview's environment or node_modules.

The new frontend workflow repeats locked installation without lifecycle scripts, audit, type checking, tests and build on Node 24 / Ubuntu 24.04. It retains audit JSON, JUnit results and the lock checksum for 14 days. No secrets, live Azure calls or deployment are involved. Node 22 or Node 24+ is declared; CI verifies Node 24. Visual/browser acceptance and real Entra sign-in remain separate from these checks. The build's existing large-chunk notice is a performance follow-up.

GitHub alerts will remain open until the patched manifests reach its default branch and dependency analysis completes. They must not be dismissed merely because this local audit passed.

References: [Zod 4 migration](https://zod.dev/v4/changelog), [Vitest 4 migration](https://v4.vitest.dev/guide/migration.html), [minimatch advisory](https://github.com/advisories/GHSA-g3hv-75wg-m249). Exact versions and peer/engine requirements were also checked against npm registry metadata.
