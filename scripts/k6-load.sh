#!/usr/bin/env bash
# k6 catalogue run with JSON + summary under .run/performance/ (gitignored).
#
#   BASE_URL=https://api-gateway-….onrender.com SCENARIO=smoke ./scripts/k6-load.sh
#   SCENARIO=load|burst|soak for the other profiles.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
SCENARIO="${SCENARIO:-smoke}"
SCRIPT="${K6_SCRIPT:-$ROOT/tests/load/catalogue-public.js}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

mkdir -p "$REPORT_DIR"
JSON_OUT="$REPORT_DIR/k6-${SCENARIO}-${STAMP}.json"
SUMMARY_OUT="$REPORT_DIR/k6-${SCENARIO}-${STAMP}.txt"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed. brew install k6" >&2
  exit 1
fi

echo "scenario=${SCENARIO} base=${BASE_URL}"
echo "json=${JSON_OUT}"

k6 run \
  --summary-export "$JSON_OUT" \
  -e BASE_URL="$BASE_URL" \
  -e SCENARIO="$SCENARIO" \
  ${API_KEY:+-e API_KEY="$API_KEY"} \
  "$SCRIPT" | tee "$SUMMARY_OUT"

echo "wrote $JSON_OUT"
echo "wrote $SUMMARY_OUT"
