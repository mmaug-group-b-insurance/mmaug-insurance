# Azure calculator estimate - 30 October 2026

Prepared visibly in the user's existing Chrome Azure Pricing Calculator. Public USD list pricing, not signed-in agreement pricing. Estimate only; no resources provisioned and no purchase made. The Excel export and screenshot are saved in the project artifact workspace output directory as Azure_Staging_Estimate_2026-10-30.xlsx and .png.

## Result

Calculator monthly total: **USD 197.37**; upfront: USD 0; annual: USD 2,368.40. Approved staging budget: USD 100/month. This candidate exceeds that budget by USD 97.37 before any unpriced design items or taxes. Do not deploy it as an approved USD 100 design.

| Service | Main assumption | Monthly USD |
| --- | --- | ---: |
| Container Apps | North Europe, consumption, scale to zero, 0.5 vCPU/1 GiB, 10,000 requests, average 3 seconds, concurrency 1 | 0.00 |
| PostgreSQL Flexible Server | North Europe, B1MS, 730 hours, 32 GiB premium SSD, LRS backup within included allocation, no HA | 17.19 |
| VPN Gateway | North Europe, VpnGw1AZ, 730 hours, no excess connections, no outbound transfer entered | 153.30 |
| Blob storage | North Europe, 10 GB hot LRS, 10,000 each write/list/read/other operations | 0.33 |
| Document Intelligence | North Europe, S0 prebuilt layout, 500 pages, no custom training/add-ons | 5.00 |
| Azure OpenAI | North Europe calculator selection, GPT 4.1 mini pricing candidate, Data Zone, 2M input/400k output tokens | 1.58 |
| Container Registry | North Europe, Basic authenticated registry, 31 days, no extra storage/inter-region transfer | 5.16 |
| Monitor | North Europe, approximately 1 GB/month analytics logs, one month retention, calculator free allocation | 0.00 |
| Key Vault | North Europe, 10,000 standard operations, no HSM | 0.03 |
| Defender for Storage | North Europe, one account for 730 hours plus 2 GB malware scanning | 10.08 |
| Public IP | North Europe, one Standard static IPv4 for 730 hours | 3.65 |
| Private DNS | Zone 1, two private zones, 0.1M queries, no Private Resolver endpoint | 1.04 |

Rounded line prices may not sum exactly to the calculator's unrounded total. The gateway contributes USD 153.30; all other entered services total approximately USD 44.07. The latter is not an alternative complete private deployment price.

## Workload and limitations

Planning workload is 100 synthetic claims and 500 pages per month. Token and request volumes are assumptions to validate against measured runs. The model selection is a pricing candidate, not a confirmed deployment, quota grant, availability claim or quality evaluation. Free allocations are shared/account-dependent and cannot be guaranteed for this subscription from an unsigned calculator session. Higher OCR/token usage, retries, logs and always-running replicas increase costs.

The candidate assumes an internal application environment with an agreed private access design. VPN authentication, DNS resolution from client laptops, TLS routing, service reachability and the relevant SKUs still require technical validation. Basic Container Registry is authenticated but does not provide a registry private endpoint; network isolation requirements for that resource must be confirmed separately.

Not included: Private DNS Resolver endpoints or a DNS forwarding host; Container Apps private endpoint/environment management fees if required by the final design; separately billable managed network/load-balancer/NAT/egress components; service private endpoints, Defender scan-event routing, queue usage and operational alert usage beyond entered meters; additional backups or disaster recovery; premium support, taxes, paid Entra licences, AI Search (scope undecided), contingency and usage growth. Final design must enumerate these, remove unused features and price required ones before approval. This is a planning baseline, not a guaranteed all-in deployment bill.

The modern full-month managed VPN candidate is over budget. Review a lower-cost private access arrangement suitable for synthetic staging, or revise the approved budget. Do not silently replace network-private access with a public web endpoint. Azure budget alerts do not impose an automatic spending stop.

The calculator tab remains open in Chrome. The estimate was exported, not saved to a signed-in Microsoft calculator account; Save/Share were disabled while unsigned. The exported workbook is the durable record of this estimate.
