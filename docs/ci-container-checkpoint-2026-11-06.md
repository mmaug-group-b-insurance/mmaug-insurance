# CI and container checkpoint — 6 November 2026

The restarted local backend returned HTTP 200 for readiness. The previous increment's 156-test run and executable-JAR acceptance are recorded in `pii-test-results-2026-11-06.json`; those tests were not repeated for these CI-only changes. The Java application and frontend source are unchanged in this checkpoint.

## Prepared

`backend-checks.yml` now runs `scripts/test-container-smoke.sh` after building the image. The smoke uses a uniquely named Docker network and PostgreSQL 16 container, synthetic seed data, stub OCR/scanning and deterministic summaries. It inherits no host provider credentials, publishes no host ports and uses no persistent volumes. Its backend runs with a read-only root filesystem, temporary writable directories, dropped capabilities and no privilege escalation. It checks runtime UID, readiness, unauthenticated claims/metrics rejection and allowed/denied CORS preflights. Cleanup targets only this run's generated names and removes anonymous database volumes.

The workflow retains test XML, the JAR checksum and successful container evidence for 14 days using a pinned upload-artifact action. It does not push an image, deploy to Azure or use live-provider secrets. Root ignore rules exclude local credentials, logs and Python caches; Docker context rules additionally exclude local configuration and key material. These rules leave underlying local files in place and do not untrack existing files.

## Verified and limits

- Actionlint 1.7.12: passed, downloaded from the official rhysd/actionlint release and checked against its published SHA-256. Shellcheck/Pyflakes integrations were disabled because those binaries were not installed.
- Bash syntax validation: passed.
- Evaluation scoring: 5 tests passed without provider calls.
- Ignore behavior: local `.env`, logs and Python cache paths ignored; frontend `.env.example` remains eligible for version control.
- Initial Docker preflight failed because the engine was unavailable. After the socket-only repair below, the Linux engine started and the image built successfully. The isolated container smoke passed: UID 999, read-only root, readiness 200, anonymous claims/metrics 401, allowed preflight 200 and denied preflight 403. All generated test containers and networks were removed. See `container-test-results-2026-11-06.json` for the image and embedded JAR hashes.
- Git Bash produced transient process-fork/resource errors during startup polling on Windows. The polling recovered and every acceptance check passed. The image was tested locally on Docker Desktop; the Linux GitHub runner remains unverified.
- GitHub: no Actions workflows/runs were returned by repository inspection. The prepared workflow and accumulated changes remain local and uncommitted. Running an older remote revision would not validate this working tree.

## Docker startup repair

After the user requested another restart, Docker Desktop repeatedly failed while opening its local inference and Secrets Engine sockets. The Linux engine had not started. Individual socket rename failed with a Windows access error. With the failed Docker Desktop processes stopped, socket-only directories were inspected and preserved by renaming the parent directories, then fresh empty directories were created. Both directories had to be cleared in the same restart because startup recreated the first socket before reaching the second failure.

Preserved backups (no recursive deletion, factory reset, image prune or WSL unregister):

- `%LOCALAPPDATA%/Docker/run.stale-20261106120636`
- `%LOCALAPPDATA%/Docker/run.stale-20261106120812`
- `%LOCALAPPDATA%/docker-secrets-engine.stale-20261106120729`
- `%LOCALAPPDATA%/docker-secrets-engine.stale-20261106120812`

The directories contained only the inspected runtime socket objects. Docker's persistent disks, images and volumes were not moved or deleted. The Linux engine subsequently returned server version 29.6.1. This is a local recovery observation, not a general instruction to rename Docker data directories.

## Repeat image acceptance

With Docker Desktop's Linux engine running, from the repository root:

```bash
docker build -t mmaug-backend:ci mmaug-claims-agent-backend
bash scripts/test-container-smoke.sh mmaug-backend:ci /tmp/mmaug-container-smoke.json
```

Use Git Bash on Windows or a Linux shell. Choose a fresh report path for each run. The script refuses stale report paths and only emits success evidence after every check passes. Its local profile deliberately permits synthetic development seed data; passing this check does not prove Entra, Defender, managed identity or private Azure staging acceptance.

Review the accumulated backend, tests, scripts, documentation and workflow changes together before publishing the branch. Keep `.env`, runtime logs, cache data and credentials out of the commit. Then open a pull request and assess the checks on that exact commit. Local container acceptance is complete; successful remote CI remains a required handoff gate, alongside the Azure gates in `pii-release-increment-2026-11-06.md`.
