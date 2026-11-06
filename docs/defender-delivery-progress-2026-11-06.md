# Defender delivery setup — 6 November 2026

## Completed configuration

User reported access grants and asked to continue. Read-only inspection verified the existing East US storage account, Event Grid topic and system-assigned topic identity. The topic accepts Event Grid Schema and has public publishing enabled, as required by the Defender integration. No topic keys or tokens were read.

The results queue and private dead-letter container already existed. The account had a blob container named `defender-results`, but the required `quarantine` and `clean` containers were absent. Both were created privately through the management plane. The existing `defender-results` blob container was left untouched; it is distinct from the queue of the same name.

Created `evgs-mmaug-malware-to-queue-stg` under `evgt-mmaug-malware-results-stg`. Azure reports Succeeded:

- Destination: `stmmaugclaimsstg01`, queue `defender-results`, system-assigned identity.
- Dead-letter destination: private `eventgrid-deadletter` blob container, system-assigned identity.
- Delivery schema: Event Grid Schema.
- Included type: Microsoft.Security.MalwareScanningResult.
- Filter: data.blobUri starts with `https://stmmaugclaimsstg01.blob.core.windows.net/quarantine/`.
- Delivery retry: 30 attempts, 1440-minute event lifetime; queue message TTL: seven days.
- Scoped topic-identity grants already exist: queue Message Sender and dead-letter-container Blob Data Contributor. Two additional account-wide duplicate grants were observed and were not changed; ask the role owner to narrow these after confirming the resource-scoped assignments meet delivery needs. They currently exceed the intended least-privilege scope.

The account still has public networking disabled, shared-key access disabled, anonymous blobs disabled and AzureServices bypass with default Deny. No private endpoints are present. Blob versioning and seven-day blob/container soft delete were confirmed. No network restriction was relaxed. The Event Grid managed-identity/trusted-service path still requires actual delivery testing; private endpoints do not provide Event Grid push delivery.

## Scan enablement blocked by Azure

Attempted the account-only configuration in `infra/staging/defender-account-settings-2026-11-06.json`: subscription override true, on-upload scanning true, 1 GB monthly volume, result topic bound, sensitive-data discovery false. No subscription-wide Defender pricing/plan resource was changed.

Azure returned HTTP success with an embedded operation failure. The returned effective configuration still has on-upload scanning **false** and cap **-1**. Therefore scanning and the proposed cap are **not applied**. Treat the provider's operation status as authoritative rather than the CLI exit code alone.

First denied action:

`Microsoft.Security/datascanners/write`

Scope:

`/subscriptions/9a2ab22e-5269-4da3-a16a-eeed2b0c09aa/providers/Microsoft.Security/datascanners/StorageDataScanner`

This is Defender's scanning-service resource at subscription scope, not permission to enable the paid Defender plan for every storage account. The administrator should perform the one-account feature enablement and its required service-identity/RBAC provisioning. Prefer that over granting this developer broad subscription Owner access. Further provisioning permissions might be reported after this first denial is resolved.

The administrator request was shown to the user in chat. Entra application metadata also remains denied to the CLI account; it is a separate later identity acceptance issue.

## Monitoring and acceptance status

Event Grid exposes publish/delivery success, failed delivery and dead-letter metrics. A read of the preceding hour returned no metric points for the four sampled series; that is **not evidence of a successful delivery or a measured zero-error run**. Metrics summary is saved in `infra/staging/event-delivery-metrics-2026-11-06.json`. No alert/action-group notification was created or tested.

After the administrator enables scanning:

1. Run `scripts/Get-DefenderDeliveryStatus.ps1`; require the effective 1 GB cap, expected topic and account-only override to match intent. The volume cap is not a dollar cap.
2. Finish private connectivity/hosting and backend managed identity; do not open storage to all networks for testing.
3. Perform a controlled synthetic scan, verify Event Grid arrival and the real Base64-decoded queue payload, then consume it with the Java adapter and confirm ETag-bound clearance. Do not manually forge a production clearance event as a substitute.
4. Test malicious/error/missing results, retry/dead-letter delivery and recovery; documents must remain unavailable to OCR/download unless clean.
5. Configure and exercise delivery-failure/dead-letter alerts and sustained backlog monitoring with a named action-group owner. Event Grid dead-lettering does not replace consumer poison-message handling.

Saved configuration metadata is in `infra/staging/event-delivery-observed-2026-11-06.json`. Source code/frontend, local preview and accepted claim history were not changed. No real document scan or model call was performed in this setup turn.

## Recheck after administrator update

At 2026-11-06 11:29 UTC, effective scanning was still disabled, cap -1, with no result topic. Retrying the saved account-only enablement again returned MissingPermissions for Microsoft.Security/datascanners/write at the subscription StorageDataScanner resource. No successful scan enablement is claimed. The administrator must verify this subscription-scope provisioning permission or directly complete account-scoped enablement; storage-account/topic delivery grants alone did not resolve the denial.
