> Current increment — 5 November 2026: application-side Azure Blob/Defender-queue adapters and V17 are now implemented and tested locally. See [private-evidence-increment-2026-11-05.md](private-evidence-increment-2026-11-05.md) and its test report. Cloud resources, managed identity, real Defender delivery and private-network/cost acceptance remain pending. Older status statements below describe earlier artifacts.
> Current status — 5 November 2026: the dated observations and implementation gaps below are historical. Use [release-readiness-2026-11-05.md](release-readiness-2026-11-05.md) and [release-verification-2026-11-05.json](release-verification-2026-11-05.json) for current release evidence. Azure CLI now confirms two AI resources in the target group; real OCR/model, grounded tools/summaries and V16 transaction separation are implemented and tested. Private storage/scanning, verified Entra, private topology/cost and cloud deployment acceptance remain open. No VPN Gateway will be deployed.
# Private staging deployment proposal

Update 2 November 2026: the user prohibits VPN Gateway deployment. The earlier gateway proposal is superseded. Review the new Entra evidence and unresolved scope/client-role mapping in [entra-network-review-2026-11-02.md](entra-network-review-2026-11-02.md).
Status: draft for review, not a deploy command. Synthetic claims only. Azure target: `rg_mmaug_insurance`, subscription `9a2ab22e-5269-4da3-a16a-eeed2b0c09aa`. Resource-group metadata is North Europe, but each service region and model deployment still require subscription/quota verification. Monthly budget ceiling: USD 100. No existing team VPN/private connection.

## Verified starting point

On 30 October 2026, signed-in Azure portal inspection showed zero resources after filters were cleared. Two successful historical deployments do not prove current infrastructure. No claim of Owner/Contributor privileges or deployment permission has been verified. App registrations are tenant objects outside the resource group: owned applications is empty and All applications is denied.

The subscription ID is not the tenant ID or a frontend/API client ID. The visible tenant ID is `71beefd7-29f4-4b9e-9cb2-e78b5dc851ad`. No application registration IDs are known.

## Candidate deployment shape

Preserve the existing Java 17/Spring Boot backend and React frontend. Build the unchanged frontend with staging settings and host it behind a private HTTPS entry point. Run backend processing in the same private application environment; browser API requests must follow the configured gateway/route, not expose the internal backend independently. Use a private PostgreSQL database, private Blob containers, real OCR and a provider-backed summary model. Keep all consequential claim actions absent.

An internal Container Apps environment with a team access route is a candidate, not an approved implementation. Establish DNS and client connectivity before creating it; internal ingress alone will not give a developer laptop access. Do not quietly substitute an Internet-accessible endpoint for the requested private environment. Frontend authentication bypass is false; backend profile is staging; credentials are obtained through approved secret/identity mechanisms.

A new VPN or controlled development access route must be costed and reviewed. Do not assume that a low-priced legacy VPN SKU supports the required client authentication or is available for new deployment. No gateway or tunnel has been selected or created.

## Budget constraints

The Azure Retail Prices API was queried for North Europe in USD; selected raw meters are preserved in `infra/staging/retail-price-evidence.json`. These are public list prices, not a binding subscription quote, tax-inclusive bill or a complete service estimate.

The catalogue lists an Environment Private Endpoint meter at USD 0.143/hour (effective 1 September 2026). At 730 hours, that meter alone is USD 104.39 if applicable. It therefore cannot be accepted as a continuously billed component within the entire USD 100 ceiling. Dedicated Plan Management is also listed at USD 0.10/hour; do not combine or select overlapping meters without checking the actual billing model. Microsoft documents management charges for private endpoints even on Consumption plans.

Do not promise that every alternative internal-network deployment fits USD 100. Validate the environment/network/access design and all recurring charges first. Count database compute/storage/backups, registry, logs, private networking/DNS/IPs, OCR pages, model tokens and egress. No model name, OCR quota or service SKU is approved by this proposal.

For planning only, reserve spending envelopes of USD 35 for hosting/private access, USD 25 for database/storage, USD 20 for OCR/model usage, USD 10 for registry/monitoring and USD 10 contingency. These are allocations, not service price estimates. If a verified design cannot fit them, revise the architecture or usage schedule before provisioning; do not exceed the approved ceiling.

Azure budget alerts do not automatically stop resources or usage. A release plan must include bounded uploads/OCR pages/model tokens, replica limits, explicit teardown of chargeable temporary resources, monitoring owner and a response procedure for alerts. Any automation that stops or deletes resources needs a reviewed implementation; none is installed here.

## Identity information required from the tenant app owner

Provide the actual frontend SPA application/client ID and backend API application/client ID; confirm tenant and issuer, API Application ID URI and delegated access scope, application roles/assignments and allowed staging redirect/logout URLs. Do not send passwords, client secrets or tokens in chat. Confirm whether existing registrations can be reused; do not create duplicates or grant tenant-wide administrator permissions as a workaround for read access.

## Application work before accepting staging

1. Align agent-run DTOs and analysis endpoints to the unchanged frontend contract; prove the officer journey through the browser.
2. Implement real private storage, malware scan-result handling and PDF/image OCR providers; test unavailable/denied/timeout cases and stable page/source references.
3. Complete run-aware fact reprocessing, controlled policy/history/rules execution and schema-validated, evidence-verified summaries.
4. Complete audience validation and agreed case assignment/access rules. Verify a second officer cannot access a case outside the agreed scope.
5. Prove exact-summary-version review, auditing, retries and rollback/restore. Run labelled synthetic evaluations and record accuracy/latency with versions.
6. Review resource names, identities, ingress, service SKUs, quotas and priced monthly usage, then prepare an infrastructure change preview. Provision only the reviewed configuration.

## Current evidence

The backend suite passes 37 tests. A candidate container built and ran with an isolated PostgreSQL database and healthy readiness/liveness; that smoke test used the local profile, not authenticated staging. CI is drafted locally and has not run on GitHub. No cloud application is deployed. Test counts and a healthy container do not establish completion of the gates above.

## Sources

- [Container Apps networking](https://learn.microsoft.com/en-gb/azure/container-apps/networking)
- [Container Apps billing](https://learn.microsoft.com/en-us/azure/container-apps/billing)
- [Private endpoints and DNS](https://learn.microsoft.com/en-us/azure/container-apps/private-endpoints-with-dns)
- [Azure budgets and their notification behaviour](https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets)
- [VPN Gateway overview](https://learn.microsoft.com/en-us/azure/vpn-gateway/vpn-gateway-about-vpngateways)

## Calculator result recorded 30 October 2026

A populated Azure calculator estimate was exported at USD 197.37/month, with USD 153.30 attributable to a full-month VpnGw1AZ gateway. It exceeds the approved USD 100 budget and has additional networking/design exclusions. See `azure-calculator-estimate-2026-10-30.md` for all entered assumptions and exclusions. No resources were created.
