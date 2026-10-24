# MMAUG Insurance Claims Agent Business Requirements

> Version 1.1 | 23 October 2026

**MMAUG Group B**

**Insurance Claims Agent**

BUSINESS REQUIREMENTS DOCUMENT (BRD)

Version 1.1  |  Meeting revision  |  23 October 2026

Prepared for: MMAUG Insurance Ltd (training simulation)

RFP reference: MMAUG-AIDB-RFP-003

Updated after the 22 October 2026 proposal review and supplied clarification responses. This revision aligns MVP scope and Agile next steps. Customer review disposition, proposed limits/targets and outstanding technical decisions remain to be recorded; no completed implementation or blanket sign-off is asserted.

## 1. Executive summary

MMAUG Insurance Ltd receives motor insurance claims with information distributed across forms, descriptions, reports, photographs and repair estimates. Officers spend time consolidating claim details, identifying omissions, checking policy and claim history, and preparing investigation summaries. This project will deliver a human-supervised Insurance Claims Agent that creates a claim case, ingests uploaded documents, extracts structured facts, detects missing/contradictory information, retrieves authorised policy/history/rules via controlled tools, and presents a traceable evidence-based summary for officer review.

The system assists claim preparation only. It does not approve or reject claims, decide final liability, authorise payment or change policy records. This boundary is non-negotiable. The MVP demonstrates secure operation and a repeatable DevSecOps pipeline with synthetic data.

## 2. Business context, problem and opportunity

| Problem from RFP | Business consequence | Expected capability |
| --- | --- | --- |
| Fragmented documents/systems | High preparation effort | Single organised digital case |
| Missing information | Investigation delays | Required-field completeness check |
| Repeated policy/history searches | Duplicated officer effort | Authorised read-only lookups |
| Inconsistent manual summaries | Variable quality | Grounded structured summary with references |
| Need for accountable AI | Risk of unsafe decisions | Human-in-the-loop and auditable tool use |

## 3. Objectives and success measures

The following figures are Group B evaluation proposals, not contractual SLAs. Q11 retains proposed extraction >90%, omission recall 90%, conflict recall 85% and 60-second latency. Agree dataset, matching rules and timing method before acceptance testing.

| Metric | Proposed MVP target | How to verify |
| --- | --- | --- |
| End-to-end case completion | All 10–15 seeded cases process to a summary or explicit recoverable error | Automated scenario suite |
| Required-field extraction | >90% exact/normalised correctness on applicable labelled fields; proposal pending measurement agreement | Field-level ground-truth evaluation |
| Missing-information detection | ≥90% recall on seeded omissions | Labelled missing-field cases |
| Contradiction detection | ≥85% recall on deliberately planted contradictions | Labelled conflicts |
| Citation support | 100% of factual assertions in generated summary have a verifiable source or are marked unavailable/uncertain | Manual groundedness review |
| Human authority | 0 AI approvals, rejections, liability determinations, payments or policy mutations | Forbidden-action integration tests |
| Reliability | Graceful failure for all simulated API outage cases | Fault-injection tests |
| Pipeline | Repeatable CI tests + a documented deployment and rollback demonstration | CI/CD run evidence |

Stretch metric: compare median preparation time for assisted vs manual workflow using the same synthetic cases. Do not claim quantified time savings until measured.

## 4. Stakeholders and accountability

| Role | Needs/decision rights |
| --- | --- |
| Simulated customer sponsor | Confirms outcomes, scope and UAT acceptance |
| Claims officer (primary user) | Creates/reviews case, flags errors, decides next operational action |
| Product owner / proxy | Orders backlog and accepts stories against criteria |
| Project manager / Scrum facilitator | Coordinates scope, risks, dependencies and ceremonies |
| AI engineer | Implements extraction, reasoning boundaries, evaluation and guardrails |
| Backend/integration engineer | Implements APIs, policy tool, persistence and authorisation |
| Frontend designer/engineer | Builds accessible workflow, evidence viewer and states |
| DevOps/security engineer | Builds pipeline, secrets, scanning, IaC, deployment and monitoring |
| QA/test owner | Manages test dataset, acceptance matrix and regression checks |

Nominate one product owner or authorised proxy to order the backlog and accept business outcomes. The project manager coordinates delivery; the role does not automatically confer product-owner authority. Assign named implementers and reviewers during refinement.

## 5. Scope and product boundaries

### 5.1 MVP in scope

- Authenticated internal claims officers create and view motor accidental-damage cases and securely upload supporting documents. Administrator access is required; supervisor/auditor access is optional with justification.

- Extract claim details into a structured schema with confidence/uncertainty indications.

- Validate required claim details and flag missing or conflicting values with their respective evidence.

- Controlled read-only lookup of synthetic policy, coverage, prior claim history and applicable business rules.

- Generate an evidence-referenced case summary separating retrieved facts, inferred observations and unknowns.

- Mandatory officer view/review and acknowledgement. Notes, corrections and request-information are desirable if capacity permits; preserve original machine output.

- Log key processing milestones, versioned prompts and tool calls; show graceful degradation on failures.

- Automated test suite, security controls, CI/CD, infrastructure definition, deployment and rollback demonstration.

### 5.2 Explicit exclusions

- Automated approval/rejection, final liability assessment, payment authorisation or policy amendment; theft, third-party claim workflows and claimant self-service are excluded from the baseline MVP.

- Payment authorisation, settlement execution or transfer initiation.

- Use of real customer data without separate approval and governance.

- Production integration to a live insurer or production-grade claims adjudication.

- Autonomous external communications to claimants (defer unless authorised).

### 5.3 Candidate phase-two scope

- Claimant portal, theft/third-party workflows, autonomous external messaging, visual damage assessment, fraud signals, multilingual support, multi-tenancy and analytics require separate scope approval.

## 6. Actors and user journeys

| Actor | Primary journey | Outcome |
| --- | --- | --- |
| Claims officer | Sign in → create claim → upload evidence → start analysis → inspect summary/evidence → review | Prepared case and transparent uncertainty |
| Supervisor/auditor optional | Inspect processing history, reviewer action and tool calls | Traceability |
| Administrator minimum | Manage role assignments and synthetic data, not final claim decisions | Controlled access |

## 7. Functional requirements and acceptance criteria

Priority: Must = baseline MVP; Should = if capacity; Could = future. Source tags: RFP = directly required; PROPOSED = solution elaboration needing approval.

| ID | Requirement | Priority | Basis | Testable acceptance criterion |
| --- | --- | --- | --- | --- |
| FR-01 | Authentication and RBAC | Must | RFP | Only authenticated users create/access authorised claims; unauthorised access returns 403. |
| FR-02 | Claim creation | Must | RFP | Internal officer creates motor accidental-damage case with stable ID. Agree minimum intake fields separately from evidence fields permitted to be missing and flagged. |
| FR-03 | Secure uploads | Must | RFP | Apply agreed PDF/JPEG/PNG parsing allowlist; propose 10 MB/file and five attachments/claim. Reject/quarantine invalid files; retain hash/source ID and clearance state. |
| FR-04 | Document extraction | Must | RFP | System emits versioned JSON fields and source positions; unsupported field remains null/uncertain. |
| FR-05 | Completeness check | Must | RFP | Required-field rules expose exact absent fields without guessing their values. |
| FR-06 | Conflict detection | Must | RFP | Identity, policy number, vehicle registration, incident date or material amount mismatch flags both sources and escalates to human review; never auto-resolve. |
| FR-07 | Read-only policy tool | Must | RFP | Authorised lookup returns synthetic policy and coverage; denied/timeout path handled. |
| FR-08 | Claim history and rules lookup | Must | RFP | Authorised tool yields relevant history/rules and attaches provenance. |
| FR-09 | Evidence-based summary | Must | RFP | Overview; extracted facts; lookup results; missing/conflicting facts; uncertainties; follow-up; citations and limitations. Distinguish facts from interpretation; no invented coverage/liability. |
| FR-10 | Human review | Must | RFP | Officer views/reviews and acknowledges an exact summary version; actor/time captured. No approval/rejection/payment action. |
| FR-11 | Processing/audit history | Must | RFP | Record actor, case/run IDs, tool/method version, action, outcome and time; exclude secrets and sensitive content; retention duration to agree. |
| FR-12 | Graceful error handling | Must | RFP | Unavailable tool triggers retry/circuit-breaker policy and visible “unavailable” state; no invented results. |
| FR-13 | Case search/list | Should | PROPOSED | Officer can filter their assigned claims and reopen previous analyses. |
| FR-14 | Review comments/corrections | Should | PROPOSED | Reviewer can add notes/corrections and preserve original machine result. |
| FR-15 | Report export | Could | PROPOSED | Officer can export summary with references and disclaimer. |

## 8. Non-functional requirements

| ID | Category | Requirement / verification |
| --- | --- | --- |
| NFR-01 | Security | TLS, encryption at rest, file allowlist, malware scan where available, input isolation; verify with tests. |
| NFR-02 | Access | Server-enforced least-privilege RBAC and object-level authorisation; negative tests. |
| NFR-03 | Privacy | Use synthetic data; mask sensitive material in logs; retention/deletion policy requires approval. |
| NFR-04 | Reliability | Idempotent processing/retries; corrupt files and offline APIs are recoverable. |
| NFR-05 | Performance | Proposed 60-second claim-analysis target; agree timing boundary/statistic and environment, then benchmark. Not an SLA. |
| NFR-06 | Explainability | Every material factual summary statement traces to a document span or authorised response. |
| NFR-07 | Accessibility | Keyboard navigable, labelled inputs/status, readable evidence viewer; smoke-test WCAG 2.2 AA patterns. |
| NFR-08 | Observability | Structured logs, trace/correlation IDs, failure counters, latency dashboards and alert plan. |
| NFR-09 | Maintainability | Version prompts/tool schemas/business rules; unit, integration and AI regression checks. |
| NFR-10 | Deployment | Reproducible dev/test/demo environments; protected deployment approval and rollback drill. |

## 9. End-to-end business process

| Step | System action | Officer control / exception |
| --- | --- | --- |
| 1 | Authenticate and create case | Reject unauthorised request |
| 2 | Upload files; check type/size/quarantine | Show rejected files |
| 3 | Parse documents into structured facts | Indicate unreadable or uncertain sections |
| 4 | Run completeness and contradiction rules | Surface missing/contradictory items |
| 5 | Invoke authorised read-only policy/history/rules tools | Show timeout/denied data as unavailable |
| 6 | Assemble evidence-linked draft summary | Separate facts, observations, unknowns |
| 7 | Officer reviews and acknowledges the preparation summary; optional annotations if selected. | No AI adjudication action exists |
| 8 | Persist summary version, evidence and audit event | Permit audit inspection by authorised role |

## 10. Information and data model

Proposed conceptual entities: User, Role, Claim, Document, ExtractedField, EvidenceReference, Policy, CoverageItem, ClaimHistoryRecord, BusinessRule, ProcessingRun, ToolInvocation, Finding, SummaryVersion, ReviewAction, AuditEvent. Each document has a synthetic claim owner, immutable upload identifier and content hash; each finding can reference one or more documentary or tool-response citations. Claims have statuses such as Created → Uploaded → Processing → Review Required → Reviewed; failure state is tracked separately. No “Approved/Rejected by AI” status exists.

| Field category | Illustrative examples | Handling |
| --- | --- | --- |
| Claim | Claim ID; reported date; incident date; location; narrative | Normalise, preserve raw text and source |
| Party/policy | Policy number; insured name; registration number | Cross-check with authorised synthetic policy lookup |
| Loss/evidence | Description; estimated repair cost; attached report/photo | Citation required; amounts never used to authorise payment |
| Findings | Missing fields; conflicts; uncertainty | Structured severity/rationale; human review |
| Audit | Run ID; tool; result; timestamp; reviewer | Minimise PII and capture prompt/tool versions |

## 11. Proposed architecture

Companion document: System Design and Architecture v1.1. It incorporates the reviewer-requested agent and retrieval design direction, with specific decisions and implementation scope tracked before affected work.

Proposed Azure-first stack: React/TypeScript, Python/FastAPI, PostgreSQL, private Blob Storage, bounded processing and authenticated synthetic read-only lookup API. The reviewer requested a multi-agent architecture and Blob-to-search indexing path. Define responsibilities, handoffs, access isolation and costs in the companion design before committing. GitHub Actions, container images and Bicep support repeatable deployment; team technology choices remain to be recorded.

Trust boundary: browser → authenticated backend → scanning/quarantine + controlled parser → constrained AI workflow → read-only tool gateway → synthetic policy store; results → grounded summary/evidence viewer. Untrusted document content is data, never a command, tool instruction or system prompt. The agent may call only allowlisted read-only tools. The summary may not trigger a write to policy/claims adjudication/payment endpoints.

## 12. AI workflow and safeguards

- Deterministic steps first: file validation, parsing, schema checks, compulsory-field checks and simple conflict rules.

- LLM optional where it adds value: converting unstructured narratives to structured fields and composing readable summaries.

- Use bounded orchestration with coordinator, extraction/validation and evidence-summary roles as the candidate response to the review. Keep deterministic checks, typed handoffs, allowlisted read-only tools and finite budgets. Confirm whether all roles must be separately implemented in the MVP.

- Use typed function calls and strict JSON schemas with validation; never treat model output as a trusted database write.

- Every extracted field and summarised factual claim records evidence IDs, document spans or tool-response records.

- Mark low-confidence or unavailable facts; escalate inconsistency/uncertainty rather than hallucinating resolution.

- Log model/prompt version, tool calls and evaluation outcomes; redact credentials/PII from traces.

- Test prompt injection embedded inside an uploaded document, including attempts to reveal secrets or override tool permissions.

## 13. Security, privacy and Responsible AI

| Control | MVP implementation / demonstration |
| --- | --- |
| Identity | Session auth and server-side RBAC; no public claim browsing |
| Authorisation | Object-level access to files, claims, summaries and lookups |
| Secret protection | Environment/secret manager; not committed to repository; CI secret scanning |
| Files | Restrict MIME/extensions and size; store private; quarantine/scan; prevent arbitrary execution |
| Injection resistance | Treat content as untrusted; prompts separate system instructions from evidence; no unrestricted shell/web tools |
| AI accountability | Visible AI-generated designation, uncertainty markers and human sign-off |
| Audit | Append-oriented event log with run IDs, review events and tool errors |
| GDPR/data governance | Synthetic data for demos; define lawful basis, retention, access/deletion approach before real data |
| Review gate | No final approval/rejection, liability or payment capabilities or exposed endpoints |

## 14. DevSecOps and environment strategy

| Stage | Policy / output |
| --- | --- |
| Version control | One GitHub repository with component folders, protected main, reviewed PRs, issue-linked commits and one ordered project backlog. |
| CI | Formatting/linting, unit/integration tests, AI regression fixture tests, dependency and secret scanning |
| Build | Pinned deps, reproducible Docker image and image vulnerability check |
| IaC | Reviewable declarative infrastructure and least-privilege service identities |
| Environments | Local dev; shared test/UAT; controlled demo/production-like deployment; isolated secrets |
| Release | Manual approval to demo environment; smoke test, health endpoint and recorded deployment version |
| Operations | Structured logs/metrics, monitored failures and document retention plan |
| Rollback | Previous application version and compatible database migration rollback drill |

## 15. MVP demo dataset and scenarios

The RFP explicitly requires approximately 20 synthetic customers/policies and 10–15 example claims with complete, incomplete and conflicting information. Proposed canonical demo suite: 12 synthetic claims—4 complete, 4 incomplete, 4 conflicting. At least one read-only API call per relevant processing run; include a denied/timeout scenario, an unsupported file scenario, and a prompt-injection document as additional tests.

| Demo scenario | Expected outcome |
| --- | --- |
| Complete claim | Fields extracted; policy reference found; cited summary; officer review |
| Incomplete claim | Missing incident date / contact / evidence identified; no fabricated values |
| Conflicting claim | Contradictory policy number or incident date raised with both citations |
| Tool service failure | Explicit unavailable status and retry path; no invented coverage |
| Prompt-injection file | Document instructions ignored; only authorised lookup permitted |
| Unauthorised user | Access blocked to another officer’s case and private documents |
| Release demonstration | CI checks shown; IaC deployment and rollback verification |

## 16. Definition of Ready and Definition of Done

### Definition of Ready

- Story states who benefits and why, is sized, has explicit acceptance criteria and test data.

- API/data dependencies and security implications identified; design or unknowns tracked.

- Owner assigned and dependency resolved or transparently blocked.

### Definition of Done

- Peer-reviewed code on protected branch; acceptance tests and negative security tests pass.

- AI feature includes fixture evaluation and source-grounding checks where applicable.

- User interface exposes errors/uncertainty accessibly; tool activity is logged.

- Reviewed integrated code; acceptance and relevant security tests pass; CI green; demonstrable in the agreed environment; documentation updated. AI stories also require fixture evaluation and source-grounding checks. Inspect the increment with stakeholders at Sprint Review.

## 17. Agile delivery plan

Next: a short requirements reconciliation and backlog refinement workshop, suggested within 2-3 working days, followed by Sprint Planning. Choose sprint length and scope from bootcamp dates and actual capacity; previous five-sprint timing is not a commitment. Maintain one ordered backlog and usable integrated increment each sprint. Discovery, design, security and testing continue throughout; Sprint 0 is not a prescribed Scrum event.

| Sprint | Sprint goal | Increment / evidence |
| --- | --- | --- |
| Preparation timebox | Reconcile requirements and refine backlog | Suggested 2-3 working days; decision log, first-sprint design, fixtures and cost assumptions |
| Sprint 1 intake | Officer signs in, creates case and uploads privately | Working UI/API path, access negatives, upload states and CI |
| Next increments | Extraction/checks and controlled lookup | Source-linked facts; labelled omission/conflict tests; API failure states |
| Next increments | Cited summary and acknowledgement | End-to-end review of an exact version; human authority preserved |
| Every sprint and final demo | Security, audit, deployment and evaluation | Integrated usable increment each sprint; final UAT, rollback and handover |

## 18. Product backlog (initial epics)

| Epic | Stories / deliverables | Relative priority |
| --- | --- | --- |
| EP-01 Identity and case intake | FR-01–03; private files; object-level access | P0 |
| EP-02 Understanding documents | FR-04–06; schema, rules, evidence locations | P0 |
| EP-03 Authorised tool integration | FR-07–08, read-only APIs, timeouts | P0 |
| EP-04 Human-centred summary | FR-09–10; cited findings and review | P0 |
| EP-05 Traceability | FR-11–12; event logging, faults | P0 |
| EP-06 Delivery controls | NFR tests, CI/CD, IaC, rollback, monitoring | P0 cross-cutting |
| EP-07 Convenience enhancements | FR-13–15 | P1/P2 |

## 19. Risks, assumptions and dependencies

| Risk / uncertainty | Impact | Mitigation / owner action |
| --- | --- | --- |
| Undefined required claims field list | Incomplete business rules | Q03 proposes field list; claims representative confirms intake versus missing-evidence rules. |
| No specified policy API | Integration blocked | Design synthetic read-only API with contract; record assumption |
| Poor extraction on photos/scans | Incorrect facts | Seed varied fixtures; fallback to manual review; evaluate OCR |
| Hallucinated coverage or liability | Safety and trust risk | Source-required summaries; restricted tool outputs; no decisions |
| Prompt injection via attachments | Tool or data misuse | Isolation, allowlist, negative tests |
| PII leakage into logs / AI vendor | Privacy breach | Use synthetic data, minimisation, vendor/data-flow review |
| Team skill / availability gaps | Delivery slippage | Pair work; P0 scope freeze; sprint capacity planning |
| Over-engineered orchestration | Wasted time | Resolve reviewer multi-agent/search request through bounded responsibilities, evidence isolation and a costed decision. |
| Deployment cost and vendor account setup | Demo failure | Low-cost fallback and early technical spike |

## 20. Clarification responses and remaining decisions

Q01 Confirmed: Motor accidental-damage claims only. Theft and third-party workflows excluded unless separately approved.

Q02 Confirmed: Authenticated internal claims officers submit claims. No claimant self-service in this MVP.

Q03 Proposed detail: Claim ID, policy number, insured/claimant name, incident date, reported date, vehicle registration, incident description and at least one supporting source. Agree minimum intake fields separately from evidence fields allowed to be missing and flagged.

Q04 Proposed limits: PDF and agreed JPEG/PNG text parsing; propose 10 MB per file and five attachments per claim. Reject or quarantine disallowed/oversized files. Confirm limits before implementation.

Q05 Clarified: Summary includes overview, extracted facts, policy/history/rule results, missing/conflicting facts, uncertainties, follow-up recommendations, citations and processing limitations.

Q06 Clarified: Flag mismatches in identity, policy number, registration, incident dates or material amounts. Preserve both sources; escalate for human review; never auto-resolve.

Q07 Clarified: Authenticated synthetic read-only API for policy, coverage, history and rules. Distinguish success, no-match, denied and unavailable; retain provenance.

Q08 Clarified: Separate extracted facts from interpretation. Do not invent missing facts, coverage decisions or liability findings.

Q09 Clarified: View/review and acknowledgement are mandatory. Notes/corrections/request-information are desirable if capacity permits. No approval/rejection/payment action.

Q10 Clarified: Claims officer and administrator minimum; optional supervisor/auditor requires justification. Backend enforces case access; exact assignment rule remains to agree.

Q11 Proposed targets: Group B: >90% extraction, 90% omission recall, 85% conflict recall and 60-second latency. Agree dataset, denominators and timing method; these are not contractual SLAs.

Q12 Partly resolved: Azure-first with justified suitable alternatives; GitHub Actions and Bicep are proposed. Submit itemised budget, prerequisites and realistic bootcamp-aligned schedule. Cap and dates remain open.

Q13 Partly resolved: Audit actor, case/run IDs, tool/method version, action, result and time. Exclude secrets/sensitive document content. Retention duration requires approval.

Q14 Confirm wording: Visible answer indicates scanned-document OCR and excludes visual damage interpretation. Screenshot right edge is clipped; confirm full wording and supported image parsing.

Q15 Open: No answer supplied for synthetic business-rule/prior-history depth. Define representative rules and history fixtures with claims representative.

## 21. Traceability and validation matrix

| RFP section | BRD sections | Primary evidence |
| --- | --- | --- |
| §3–4 Business problem/objectives | 2–3 | Agreed metrics and business scenario demo |
| §5 Functional requirements | 5–10 | FR-01–12 test suite |
| §6 Boundaries | 5, 12–13 | Negative tests on forbidden actions |
| §7 Security/privacy | 8, 13–14 | Threat model and security checks |
| §8 Responsible AI | 9, 12–13 | Grounding and injection evaluation |
| §9 DevOps | 14, 17 | Pipeline, IaC, monitoring and rollback |
| §10 Demo | 15 | 12 seeded claims, policy fixtures and human review |
| Response requirements / evaluation | 11–20 | Architecture, backlog, UAT, handover dossier |

## 22. Approval and next actions

- Hold a 90-minute refinement workshop with product owner/proxy, delivery facilitator, technical, UI, backend, AI/data, platform and QA owners.

- Reconcile Q01-Q15 responses, record conditions and nominate acceptance authority. Confirm full Q14 wording and unresolved Q15 detail.

- Update the MVP scope, field/file rules, architecture decisions, synthetic fixtures and cost worksheet. Record owner and due date for each unresolved item.

- Refine first-sprint wireframes, access rules, data/API contracts and upload failure paths; use timeboxed spikes for unresolved technical risks.

- Prepare one repository and project backlog with small stories, FR/NFR/Q links, acceptance criteria, owners, reviewers and dependencies. Product owner orders; developers estimate and select achievable work at Sprint Planning.

- Deliver and demonstrate the first integrated increment, inspect stakeholder feedback, adapt the backlog and hold a retrospective. Record approval conditions for affected implementation and cloud spending.

## 23. Phase 1 approval record

Record approve, approve with conditions, or return for revision, with name/date and scope. The meeting provides review feedback and advice to prepare issues; it is not recorded here as unconditional approval of all architecture, budget or targets. Backlog refinement can proceed; unresolved conditions are attached to affected work.

| Approval role | Name, signature, decision and date |
| --- | --- |
| Simulated customer sponsor or bootcamp delegate | Pending nomination and approval |
| Claims operations representative | Pending nomination and approval |
| Product owner or authorised proxy | Pending nomination and approval |
| Technical lead | Pending nomination and approval |

Conditions and accepted assumptions: record the related question IDs, decision, accountable owner and due date in the controlled decision log before sign-off.

Revision 1.1 incorporates the 22 October review and written Q&A, retains FR-01 to FR-15 and NFR-01 to NFR-10, narrows scope, refines review actions and metrics, updates the architecture direction and adds the ordered backlog, Sprint 1 goal and decision register.

## 24 Agile refinement and Sprint 1

Product Goal: help internal officers prepare motor accidental-damage claims using traceable evidence, clear omissions/conflicts and controlled policy lookups, while retaining all consequential decisions with people.

| Workshop step | Output |
| --- | --- |
| 15 minutes scope | Reconciled MVP and review conditions |
| 20 minutes decisions | First-sprint blockers and architecture/budget owners |
| 30 minutes stories | Small user stories with criteria, fixtures and dependencies |
| 15 minutes sizing | Product owner orders; developers estimate against capacity |
| 10 minutes planning readiness | Sprint Goal proposal, quality expectations and action owners |

Sprint 1 Goal: an authenticated officer can create a motor accident case, upload a permitted synthetic document, reopen it securely and see processing status, while unauthorised access is blocked. Select US-01 to US-03 and necessary enabling work according to capacity. Add one-format extraction only if achievable.

Ready means value, acceptance criteria, fixtures and dependencies are understood and the story fits the sprint. Done means reviewed integrated code, passing acceptance/security checks, CI, demonstrable behaviour and updated documentation; AI work includes groundedness evaluation. Security, audit and testing apply from the first increment.

Cadence: Sprint Planning sets why/what/how; a daily 15-minute developer sync inspects progress; refinement continues as needed; Sprint Review inspects the increment with stakeholders; retrospective improves delivery. Proposed board: Backlog, Ready, In progress, Review/test, Done, with a blocked flag. Assign one implementer and reviewer per story without creating component silos.

## 25 Initial ordered product backlog

Draft order for team refinement. Roles below are proposed responsibilities, not named assignments or commitments. Estimate and assign at refinement; document priorities and dependencies. EN-03 gathers release evidence but does not defer quality controls.

| ID and work | Acceptance evidence and requirement link | Owner |
| --- | --- | --- |
| EN-01 Reconcile scope and design | D01-D07; record decisions and identify blockers affecting Sprint 1. | PO + tech lead |
| EN-02 Repository and CI skeleton | One repo, component folders, reviewed PR workflow, automated check and repeatable setup. | Platform |
| US-01 Officer sign-in and case access | FR-01; valid sign-in works; unauthenticated and cross-officer case/file access fail. | Backend + UI |
| US-02 Create and reopen claim | FR-02; stable ID, agreed intake checks; missing evidence is visible without invented values. | Backend + UI |
| US-03 Upload evidence securely | FR-03; type/size/count rules, private storage, quarantine, rejection/retry messages. | Backend + platform |
| US-04 Extract facts with sources | FR-04; versioned raw/normalised fields, source locations and uncertainty; labelled fixtures. | AI/data + QA |
| US-05 Find omissions and conflicts | FR-05-06; seeded issues flagged; both conflicting sources retained; human escalation. | AI + backend |
| US-06 Read-only policy lookup | FR-07-08/12; success/no-match/denied/unavailable tests, scoped access and provenance. | Backend + AI |
| US-07 Cited preparation summary | FR-09; agreed sections, supported facts, explicit unknowns and working citations. | AI + UI |
| US-08 Acknowledge exact version | FR-10-11; actor/time/run/version saved; machine result preserved; no adjudication action. | Backend + UI |
| US-09 Audit and recover failures | FR-11-12; safe events, correlation IDs, bounded retries and honest partial/error states. | Platform + backend |
| EN-03 Release and handover evidence | NFR-01-10; UAT, CI/CD, deployment smoke check, rollback and runbooks. | Platform + QA |

## 26 Decision register and commercial readiness

| Decision | Accountable role | Required output |
| --- | --- | --- |
| D01 Scope and authority | Product owner / customer proxy | Record review disposition and conditions; nominate acceptance authority and case assignment rule. |
| D02 Fields and files | Claims representative + backend | Agree intake/evidence distinction, field dictionary, Q04 limits and full Q14 OCR wording. |
| D03 Backend and orchestration | Technical lead + AI lead | Confirm Python/FastAPI recommendation; reviewer requested multiple agents; agree MVP implementation versus target design. |
| D04 Retrieval and API | AI/data + backend | Confirm Blob-to-search scope, isolation and API schema; settle Q15 rule/history depth. |
| D05 Environment and budget | Platform + budget authority | Confirm access, region, model quota, scanner, cost cap and deployment bill of materials. |
| D06 Evaluation and retention | QA + product owner | Agree measurement methods, target acceptance, retention duration and deletion scope. |
| D07 Delivery capacity | Project manager + team | Name owners/reviewers, available capacity and bootcamp milestone dates; order first-sprint work. |

Each decision record must include status, authority, date, rationale, affected requirements/stories and due date. Owners and dates are assigned during refinement. A proposed value is not an agreed commitment.

Cost worksheet: identify region, environment, service/SKU, quantity, usage assumptions, monthly cost, demo-duration cost, currency, estimate date and budget owner. Include compute, database, storage/transactions, AI tokens, OCR pages, search, registry, monitoring, networking and any gateway. Use current calculator outputs before approval; no unsupported price or total is asserted here.

Source record: proposal review call of 22 October 2026; supplied Q01-Q14 screenshot; supplied BRD and System Design PDFs. Q14 is clipped and Q15 has no supplied answer. Scrum method reference: https://scrumguides.org/scrum-guide.html. The suggested workshop and timebox are team practices, not prescribed Scrum events.
