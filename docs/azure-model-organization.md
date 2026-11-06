# Optional Azure model organization — 4 November 2026

## Status and scope

Prepared an optional Azure OpenAI provider behind ClaimAnalysisService. This is a partial increment toward issue #69, not completed free-form LLM summary generation. The user confirmed there is no model resource/deployment yet. No resource was created, no key obtained, no model request made and no Azure spend incurred by this increment. The existing deterministic summary mode remains the default and existing frontend interfaces are unchanged.

The model may reorder verified statement IDs and select up to three source-linked overview highlights. Every input statement must remain exactly once in the main body; highlights repeat existing supported statements. Factual text and citations remain backend-controlled and cannot be rewritten by model output. All conflict values are therefore retained. Free-form narrative, coverage interpretation, liability or consequential actions are not accepted. This deliberately preserves the exact-source verifier rather than presenting arbitrary prose as semantically verified.

## Provider behavior

AzureClaimAnalysis uses a single v1 chat-completions request with strict JSON Schema output, no tools, no redirects, 10-second connection timeout and 60-second request timeout. It accepts a root HTTPS *.openai.azure.com or *.services.ai.azure.com endpoint, or the same endpoint with /openai/v1. The deployment name is explicit and limited to 40 characters by current persisted model-label limits. No model is selected or provisioned automatically.

Limits: 80 verified input statements, 64 KiB serialized request, 2000 maximum completion tokens, 128 KiB accepted response characters, zero automatic retries. Oversized inputs fail rather than silently dropping facts. No provider error bodies or secrets are returned to the user. Denied/rate-limited/unavailable/timed-out/refused/incomplete/malformed output fails closed; it does not masquerade as a successful deterministic fallback. Safe error code is recorded on the failed run. Actual provider usage, when reported, is stored on the run; deployment/provider-model/plan version are retained as metadata. No independent accuracy or latency target is asserted.

## Enable only after resource/region/quota/cost review

Backend process settings:

- AI_SUMMARY_PROVIDER=azure (default is deterministic)
- AI_ENDPOINT=https://<resource>.openai.azure.com
- AI_DEPLOYMENT=<deployed model name supporting strict structured outputs>
- AI_KEY supplied only through a hidden local prompt or deployment secret store, never VITE variables/source control/chat

Document Intelligence credentials are separate and cannot authenticate this provider. The local profile now preserves AI_ENDPOINT and AI_DEPLOYMENT environment configuration. Changing settings requires a backend restart. Do not enable Azure mode until all settings are present: startup intentionally fails on invalid/missing credentials or endpoint. Keep synthetic data only and the USD 100/month staging budget/no-VPN instruction unchanged.

This adapter uses resource-key authentication initially. Managed identity, approved network access and private staging secrets are deployment follow-ups, not silently enabled features. A model resource and compatible deployed model, regional availability/quota, expected token workload and reviewed pricing are prerequisites to live acceptance. No recommendation to create a paid deployment is implied by this document.

## Verification

Full suite: 95 tests run, 94 passed, zero failures/errors, one deliberately skipped Azure OCR live test. Two isolated temporary PostgreSQL databases were used and removed; the existing preview database was unchanged. AzureClaimAnalysisTest adds nine contract/negative checks covering source-preserving organization, duplicate/invented IDs, arbitrary prose, denial sanitation, refusal/truncation, timeout/no retry, endpoint validation and input limits. Azure provider responses are mocked: real-model compatibility and quality remain unverified. Existing four full HTTP/PostgreSQL workflow tests run in explicit deterministic mode to prevent accidental paid calls.

## Sources

- Microsoft Azure structured outputs and v1 deployment configuration: https://learn.microsoft.com/en-us/azure/foundry/openai/how-to/structured-outputs
- Official OpenAI structured-output contract: https://developers.openai.com/api/docs/guides/structured-outputs

## Next acceptance step

Prepare and review an Azure model deployment proposal against current regional quota, compatible model versions, private staging design and the USD 100/month target. Once the resource/deployment exists and a backend credential is configured securely, run one synthetic case in Azure mode, verify provider usage/model metadata, every retained statement/citation and truthful failure behavior. Keep issue #69 open until real-model acceptance and desired narrative scope are agreed.

## User-reported deployment — 4 November 2026

Screenshots supplied by the user report resource mmaug-claims-openai-01, endpoint https://mmaug-claims-openai-01.openai.azure.com/, East US, deployment/model gpt-6.1-sol version 2026-09-29, Global Standard, provisioning succeeded. Networking: All networks; no private endpoints. Reported quota 250,000 TPM / 250 RPM is a throughput limit, not a USD budget cap. These settings have not been independently inspected by the agent and do not satisfy the agreed private staging design. Global Standard does not guarantee inference processing in East US. Keep synthetic inputs only until deployment/network/data handling acceptance is resolved.

AzureModelLiveTest is an explicit opt-in, one-request synthetic provider smoke test using AI_ENDPOINT, AI_DEPLOYMENT and backend AI_KEY. Enable MMAUG_RUN_LIVE_MODEL=true only for that test. It checks preserved source text/citations and complete statement coverage; it does not prove full application integration or free-form narrative quality. The normal sprint-check script disables both live OCR and live model tests. Nine mocked provider tests passed; live model test was skipped pending secure key configuration. API-key material has not been requested in chat or obtained by the agent.

## Live Azure application acceptance — 4 November 2026

User supplied a screenshot showing AzureModelLiveTest: one test run, zero failures/errors/skips, BUILD SUCCESS. After the user enabled Azure mode and restarted the backend, the agent invoked one analysis of already-extracted synthetic CLM-2026-000004.

Run 9e99875a-dfb3-4914-9914-196735021e13 succeeded with ESCALATED human-review status and no error code. Fresh API retrieval confirmed model azure-openai/gpt-6.1-sol, provider-reported 545 input tokens and 111 output tokens, and summary version 3. The persisted summary contains 14 source-linked statements (11 retained statements plus 3 model-selected overview highlights), zero without citations. The model organized existing verified quotations; it did not produce unrestricted factual prose. No new OCR was required. Warnings correctly retain pending business-rule tool/coverage interpretation and three evidence-policy discrepancies. This verifies one synthetic application call and retrieval, not production quality, calibrated accuracy, cost compliance or private staging readiness.
