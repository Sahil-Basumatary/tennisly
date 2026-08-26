#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
ORIGIN_PORT="${HTTP_LIVE_ORIGIN_PORT:-18080}"
EDGE_PORT="${HTTP_LIVE_EDGE_PORT:-18081}"
UPSTREAM_PORT="${HTTP_LIVE_UPSTREAM_PORT:-}"
CLIENTS="${HTTP_LIVE_CLIENTS:-100}"
BASE_URL="${BASE_URL:-http://127.0.0.1:${EDGE_PORT}}"
POINT_INTERVAL_S="${HTTP_LIVE_POINT_INTERVAL_S:-20}"
HOLD_S="${HTTP_LIVE_HOLD_S:-25}"
RAMP_S="${HTTP_LIVE_RAMP_S:-3}"
SUMMARY="$REPORT_DIR/k6-live-http-${CLIENTS}-${RUN_ID}.json"
OUTPUT="$REPORT_DIR/k6-live-http-${CLIENTS}-${RUN_ID}.txt"
EDGE_LOG="$REPORT_DIR/live-http-edge-${RUN_ID}.log"

mkdir -p "$REPORT_DIR"

if [[ ! "$CLIENTS" =~ ^[0-9]+$ ]] || [[ "$CLIENTS" -lt 1 ]]; then
  echo "HTTP_LIVE_CLIENTS must be a positive integer" >&2
  exit 1
fi
if [[ "$CLIENTS" -gt 2000 ]]; then
  echo "Refusing more than 2000 local HTTP live clients on one laptop" >&2
  exit 1
fi

HOST="$(python3 - <<'PY' "$BASE_URL"
import sys
from urllib.parse import urlparse
print(urlparse(sys.argv[1]).hostname or "")
PY
)"
if [[ "$HOST" != "127.0.0.1" && "$HOST" != "localhost" && "${ALLOW_VERCEL_LIVE_HTTP:-0}" != "1" ]]; then
  echo "Refusing remote BASE_URL without ALLOW_VERCEL_LIVE_HTTP=1 (Hobby quotas)" >&2
  exit 1
fi

EDGE_PID=""
cleanup() {
  if [[ -n "$EDGE_PID" ]]; then
    kill "$EDGE_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

wait_for_url() {
  local url="$1"
  for _ in $(seq 1 25); do
    if curl -sf "$url" >/dev/null; then
      return 0
    fi
    sleep 0.2
  done
  return 1
}

if [[ "$HOST" == "127.0.0.1" || "$HOST" == "localhost" ]]; then
  edge_args=(--edge-port "$EDGE_PORT" --point-interval "$POINT_INTERVAL_S")
  if [[ -n "$UPSTREAM_PORT" ]]; then
    edge_args+=(--upstream-port "$UPSTREAM_PORT")
  else
    edge_args+=(--origin-port "$ORIGIN_PORT")
  fi
  python3 "$ROOT/scripts/live-http-edge.py" "${edge_args[@]}" >"$EDGE_LOG" 2>&1 &
  EDGE_PID=$!
  if [[ -n "$UPSTREAM_PORT" ]]; then
    wait_for_url "http://127.0.0.1:${EDGE_PORT}/metrics" || true
  else
    wait_for_url "http://127.0.0.1:${ORIGIN_PORT}/metrics" || true
  fi
fi

k6_status=0
k6 run \
  --summary-export "$SUMMARY" \
  -e BASE_URL="$BASE_URL" \
  -e HTTP_LIVE_CLIENTS="$CLIENTS" \
  -e HTTP_LIVE_HOLD_S="$HOLD_S" \
  -e HTTP_LIVE_RAMP_S="$RAMP_S" \
  -e MATCH_ID="${MATCH_ID:-aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee}" \
  "$ROOT/tests/load/live-http-cache.js" | tee "$OUTPUT" || k6_status=$?

if [[ "$HOST" == "127.0.0.1" || "$HOST" == "localhost" ]]; then
  if [[ -n "$UPSTREAM_PORT" ]]; then
    curl -sf "http://127.0.0.1:${EDGE_PORT}/metrics" | tee "$REPORT_DIR/live-http-origin-${CLIENTS}-${RUN_ID}.json" || true
  else
    curl -sf "http://127.0.0.1:${ORIGIN_PORT}/metrics" | tee "$REPORT_DIR/live-http-origin-${CLIENTS}-${RUN_ID}.json" || true
  fi
fi

echo "wrote $SUMMARY"
exit "$k6_status"
