#!/usr/bin/env bash
# Post-deploy verification for Phase 8a (gateway + token-locked backends + Neon).
#
# Prefer GATEWAY_URL — that is what Vercel and users hit. Backends alone will
# 401 without GATEWAY_INTERNAL_TOKEN once the lock is enabled.
#
#   export GATEWAY_URL=https://api-gateway-xxxx.onrender.com
#   ./scripts/verify-deployment.sh
#
# Optional seed (calls tennis-data directly with the shared secret):
#   export TENNIS_DATA_URL=https://tennis-data-service-xxxx.onrender.com
#   export GATEWAY_INTERNAL_TOKEN=...
#   SEED=true ./scripts/verify-deployment.sh
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-}"
TENNIS_DATA_URL="${TENNIS_DATA_URL:-}"
MATCH_URL="${MATCH_URL:-}"
GATEWAY_INTERNAL_TOKEN="${GATEWAY_INTERNAL_TOKEN:-}"
SEED="${SEED:-false}"
WARMUP_SECONDS="${WARMUP_SECONDS:-90}"
MATCH_REQUIRED="${MATCH_REQUIRED:-false}"

failures=0

log()  { printf '  %s\n' "$*"; }
pass() { printf '\033[32mPASS\033[0m %s\n' "$*"; }
fail() { printf '\033[31mFAIL\033[0m %s\n' "$*"; failures=$((failures + 1)); }
warn() { printf '\033[33mWARN\033[0m %s\n' "$*"; }

require_url() {
  local name="$1" value="$2"
  if [[ -z "$value" ]]; then
    echo "set $name (see docs/deploy.md)" >&2
    exit 2
  fi
  if [[ "$value" != https://* && "$value" != http://* ]]; then
    echo "$name must include the scheme, got: $value" >&2
    exit 2
  fi
}

if [[ -n "$GATEWAY_URL" ]]; then
  require_url GATEWAY_URL "$GATEWAY_URL"
  CATALOGUE_BASE="$GATEWAY_URL"
else
  require_url TENNIS_DATA_URL "$TENNIS_DATA_URL"
  require_url MATCH_URL "$MATCH_URL"
  CATALOGUE_BASE=""
fi

command -v jq >/dev/null 2>&1 || { echo "jq is required (brew install jq)" >&2; exit 2; }

auth_headers=()
if [[ -n "$GATEWAY_INTERNAL_TOKEN" ]]; then
  auth_headers=(-H "X-Gateway-Token: ${GATEWAY_INTERNAL_TOKEN}")
fi

wait_for_health() {
  local name="$1" base="$2" deadline=$((SECONDS + WARMUP_SECONDS)) code
  while (( SECONDS < deadline )); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$base/actuator/health" || true)"
    if [[ "$code" == "200" ]]; then
      pass "$name healthy"
      return 0
    fi
    sleep 5
  done
  fail "$name never became healthy within ${WARMUP_SECONDS}s"
  return 1
}

check_collection() {
  local name="$1" url="$2" required="${3:-true}" body count
  # Empty array + set -u → unbound; expand only when headers exist.
  body="$(curl -s --max-time 30 ${auth_headers[@]+"${auth_headers[@]}"} "$url" || true)"

  if ! jq -e . >/dev/null 2>&1 <<<"$body"; then
    fail "$name did not return JSON"
    log "${body:0:200}"
    return 1
  fi

  count="$(jq 'if type == "array" then length elif has("content") then (.content | length) else 0 end' <<<"$body")"

  if [[ "$count" -gt 0 ]]; then
    local unit="rows"
    [[ "$count" -eq 1 ]] && unit="row"
    pass "$name returned $count $unit"
    return 0
  fi

  if [[ "$required" == "true" ]]; then
    fail "$name returned an empty result set"
    return 1
  fi

  warn "$name is empty (not required)"
  return 0
}

# Assert backends reject anonymous catalogue hits once the token lock is on.
check_backend_locked() {
  local name="$1" url="$2" code
  [[ -z "$url" ]] && return 0
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$url" || true)"
  if [[ "$code" == "401" ]]; then
    pass "$name rejects anonymous callers"
  elif [[ "$code" == "200" ]]; then
    fail "$name still open without X-Gateway-Token (set GATEWAY_INTERNAL_TOKEN on Render)"
  else
    warn "$name lock check got HTTP $code (expected 401 once token is set)"
  fi
}

trigger_sync() {
  local kind="$1" code
  if [[ -z "$TENNIS_DATA_URL" ]]; then
    fail "SEED=true needs TENNIS_DATA_URL (sync is not public on the gateway)"
    return 1
  fi
  if [[ -z "$GATEWAY_INTERNAL_TOKEN" ]]; then
    fail "SEED=true needs GATEWAY_INTERNAL_TOKEN for the locked tennis-data URL"
    return 1
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 120 -X POST \
    -H "X-Gateway-Token: ${GATEWAY_INTERNAL_TOKEN}" \
    "$TENNIS_DATA_URL/api/tennis/sync/$kind" || true)"
  if [[ "$code" == "200" || "$code" == "202" ]]; then
    pass "sync $kind accepted"
  else
    fail "sync $kind returned HTTP $code"
  fi
}

echo "== health =="
if [[ -n "$GATEWAY_URL" ]]; then
  wait_for_health "api-gateway" "$GATEWAY_URL" || true
fi
if [[ -n "$TENNIS_DATA_URL" ]]; then
  wait_for_health "tennis-data-service" "$TENNIS_DATA_URL" || true
fi
if [[ -n "$MATCH_URL" ]]; then
  wait_for_health "match-service" "$MATCH_URL" || true
fi

if [[ -n "$TENNIS_DATA_URL" || -n "$MATCH_URL" ]]; then
  echo
  echo "== backend lock =="
  # Catalogue GETs without the secret must fail once Render has the token set.
  check_backend_locked "tennis-data" "${TENNIS_DATA_URL:+$TENNIS_DATA_URL/api/tennis/players}"
  check_backend_locked "match-service" "${MATCH_URL:+$MATCH_URL/api/matches}"
fi

if [[ "$SEED" == "true" ]]; then
  echo
  echo "== seed =="
  trigger_sync players
  trigger_sync rankings
  trigger_sync tournaments
fi

echo
echo "== catalogue =="
if [[ -n "$CATALOGUE_BASE" ]]; then
  # Through the gateway: no token header — the gateway stamps it outbound.
  auth_headers=()
  check_collection "players" "$CATALOGUE_BASE/api/tennis/players" true || true
  check_collection "rankings (ATP singles)" "$CATALOGUE_BASE/api/tennis/rankings?gender=MEN" true || true
  check_collection "tournaments" "$CATALOGUE_BASE/api/tennis/tournaments" true || true
  check_collection "shot distributions" "$CATALOGUE_BASE/api/tennis/shot-distributions" true || true
  check_collection "matches" "$CATALOGUE_BASE/api/matches" "$MATCH_REQUIRED" || true
else
  check_collection "players" "$TENNIS_DATA_URL/api/tennis/players" true || true
  check_collection "rankings (ATP singles)" "$TENNIS_DATA_URL/api/tennis/rankings?gender=MEN" true || true
  check_collection "tournaments" "$TENNIS_DATA_URL/api/tennis/tournaments" true || true
  check_collection "shot distributions" "$TENNIS_DATA_URL/api/tennis/shot-distributions" true || true
  check_collection "matches" "$MATCH_URL/api/matches" "$MATCH_REQUIRED" || true
fi

echo
if (( failures > 0 )); then
  printf '\033[31m%s check(s) failed\033[0m\n' "$failures"
  echo "Empty catalogue? Run again with SEED=true + GATEWAY_INTERNAL_TOKEN, and confirm provider keys on Render."
  exit 1
fi

printf '\033[32mdeployment verified\033[0m\n'
