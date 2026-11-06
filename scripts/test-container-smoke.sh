#!/usr/bin/env bash
# Synthetic-only, isolated image acceptance. No host ports, cloud calls or persistent volumes.
set -euo pipefail
# Keep Linux container paths intact when invoking docker.exe from Git Bash on Windows.
export MSYS_NO_PATHCONV=1
image="${1:-mmaug-backend:ci}"
report="${2:-/tmp/mmaug-container-smoke.json}"
if [[ -e "$report" ]]; then echo 'Use a new smoke report path to avoid stale evidence' >&2; exit 1; fi
suffix="$(date +%s)-$$"
network="mmaug-smoke-${suffix}"
database="mmaug-smoke-db-${suffix}"
backend="mmaug-smoke-api-${suffix}"
password="$(openssl rand -hex 24)"
network_created=false
cleanup() {
  if [[ "$network_created" == true ]]; then
    timeout 15s docker rm -f -v "$backend" "$database" >/dev/null 2>&1 || true
    timeout 15s docker network rm "$network" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT
if ! timeout 20s docker info >/dev/null 2>&1; then
  echo 'Docker engine unavailable after 20 seconds; image acceptance not executed' >&2
  exit 1
fi
docker image inspect "$image" >/dev/null
docker network create "$network" >/dev/null
network_created=true
docker run -d --name "$database" --network "$network" --network-alias database \
  -e POSTGRES_USER=synthetic -e POSTGRES_PASSWORD="$password" -e POSTGRES_DB=claims \
  postgres:16-alpine >/dev/null
ready=false
for ((i=0; i<60; i++)); do
  if docker exec "$database" pg_isready -U synthetic -d claims >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then echo 'Synthetic database readiness failed' >&2; exit 1; fi
# Explicit stub providers; no host environment, credentials or document storage are inherited.
settings="{\"spring.profiles.active\":\"local\",\"spring.datasource.url\":\"jdbc:postgresql://database:5432/claims\",\"spring.datasource.username\":\"synthetic\",\"spring.datasource.password\":\"${password}\",\"spring.security.oauth2.resourceserver.jwt.issuer-uri\":\"\",\"app.security.enabled\":true,\"app.security.cors.allowed-origins\":[\"https://claims.example.test\"],\"app.docintel.provider\":\"stub\",\"app.ai.summary-provider\":\"deterministic\",\"app.storage.provider\":\"local\",\"app.storage.base-path\":\"/app/storage\",\"app.scanner.provider\":\"stub\"}"
docker run -d --name "$backend" --network "$network" --read-only --cap-drop ALL \
  --security-opt no-new-privileges --tmpfs /tmp:rw,nosuid,nodev,mode=1777 \
  --tmpfs /app/logs:rw,nosuid,nodev,mode=1777 --tmpfs /app/storage:rw,nosuid,nodev,mode=1777 \
  -e SPRING_APPLICATION_JSON="$settings" -e MMAUG_RUN_LIVE_OCR=false -e MMAUG_RUN_LIVE_MODEL=false \
  "$image" >/dev/null
code() {
  docker exec "$backend" curl -s -o /dev/null -w '%{http_code}' "$@" || true
}
ready=false
for ((i=0; i<120; i++)); do
  if [[ "$(code http://127.0.0.1:8080/actuator/health/readiness)" == 200 ]]; then ready=true; break; fi
  if [[ "$(docker inspect -f '{{.State.Running}}' "$backend")" != true ]]; then
    echo 'Candidate exited before readiness; container smoke failed' >&2; exit 1
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then echo 'Candidate readiness timed out' >&2; exit 1; fi
uid="$(docker exec "$backend" id -u)"
if [[ "$uid" == 0 ]]; then echo 'Runtime must not run as root' >&2; exit 1; fi
expect_code() {
  local label="$1" expected="$2" actual
  shift 2
  actual="$(code "$@")"
  if [[ "$actual" != "$expected" ]]; then
    echo "$label: expected HTTP $expected, received $actual" >&2; exit 1
  fi
}
expect_code anonymous-claims 401 http://127.0.0.1:8080/api/v1/claims
expect_code anonymous-metrics 401 http://127.0.0.1:8080/actuator/metrics
expect_code allowed-cors 200 -X OPTIONS -H 'Origin: https://claims.example.test' -H 'Access-Control-Request-Method: GET' http://127.0.0.1:8080/api/v1/claims
expect_code denied-cors 403 -X OPTIONS -H 'Origin: https://attacker.example.test' -H 'Access-Control-Request-Method: GET' http://127.0.0.1:8080/api/v1/claims
image_id="$(docker image inspect -f '{{.Id}}' "$image")"
printf '{"imageId":"%s","runtimeUid":%s,"readiness":200,"anonymousClaims":401,"anonymousMetrics":401,"allowedPreflight":200,"deniedPreflight":403,"readOnlyRoot":true,"providerCalls":false,"scope":"Synthetic local-profile container smoke; not Entra or Azure staging acceptance"}\n' "$image_id" "$uid" > "$report"
echo "Isolated container smoke passed; evidence: $report"
