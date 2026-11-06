# PII field recovery and staging key hardening — 6 November 2026

This increment addresses readable claim reopening and secure field storage under FR-01–03 / NFR-01–03. It preserves the Java backend, HTTP contracts, frontend source, database schema and existing ciphertext format. Azure Defender enablement remains a deferred release blocker; this increment does not change cloud resources or establish staging acceptance.

## Defect and correction

`AttributeEncryptor` measured the GCM authentication tag in bits (128) when checking a byte array. It therefore treated ciphertext shorter than 140 bytes as invalid, although an empty encrypted value needs only 28 bytes (12-byte IV plus 16-byte tag). Local fallback returned those encrypted strings to callers; shared production profiles rejected them. The minimum-length check now converts bits to bytes. Existing correctly encrypted short values can be decrypted with their original key without rewriting rows.

This does not establish that every historical unreadable value has this cause. Values encrypted with another key, tampered values or previously double-encrypted data need separate diagnosis. Do not overwrite claim records to make the screen look correct.

## Shared-environment key contract

- Staging, production and any profile outside an explicit local/dev/test-only set require `app.crypto.pii-key` or `PII_ENCRYPTION_KEY` from backend secret configuration. A mixed staging/local profile remains strict.
- The configured key must be exactly 32 UTF-8 bytes. There is no padding or truncation in shared environments. The known development fallback is forbidden there.
- Plaintext, truncated payloads and authentication failures are rejected in shared environments. Local/dev/test retain historical seed compatibility and legacy development-key normalization.
- Store a generated high-entropy key through the selected secret manager, grant only the backend identity access, and keep it out of chat, logs, Git and frontend VITE variables. A 32-character random ASCII value is 32 UTF-8 bytes; this contract does not Base64-decode the configured key.
- Retain the same key across restarts and revisions. Replacing it makes existing fields unreadable. Key rotation and restoration require a reviewed migration plus access to the historical key, rather than simply changing an environment variable.

Ciphertext remains Base64(12-byte IV || ciphertext || 16-byte GCM tag). There is no schema migration or key rotation in this increment. Earlier JARs retain the short-field decoding defect; retaining a binary alone does not make rollback behavior equivalent.

## Verification and preview

See `pii-test-results-2026-11-06.json` for the completed run and artifact checksum. Tests cover empty/short/Unicode values, random IVs, independent legacy-format ciphertext, missing/invalid keys, mixed profiles, tampering, wrong keys and local seed compatibility. The HTTP regression creates a synthetic claim, reopens it through a separate request, checks list rendering data and verifies that name, phone and email are encrypted in PostgreSQL.

The running credential-bearing preview is left running. After verification, the existing backend terminal can be restarted using `scripts/Start-AzureOcrPreview.ps1 -UseExistingSyntheticDatabase`. No new key should be configured for the existing local database as part of this fix. Reopen a synthetic case and check name, phone and email; existing summaries and reviews must remain intact.

## Remaining handoff gates

1. Review and commit the accumulated local changes, then run remote CI and the container build. Local executable-JAR tests do not prove the Docker image.
2. Complete account-scoped Defender scanning, real clean/infected event delivery, private connectivity/DNS and managed-identity permissions. Test retries, dead-letter handling and operational alerts against Azure.
3. Complete Entra sign-in/token/audience/role validation with bypass disabled. Confirm case-assignment access policy with the team.
4. Deploy synthetic-only staging within the approved budget, exercise upload → scan → extraction → cited summary → exact-version review, and verify storage plus database restore and deployment rollback.
5. Record the team release decision and operational owners. Real customer claims remain outside the accepted staging scope.
