#!/usr/bin/env bash
# Lighthouse against a warm origin. Writes .run/performance/ (gitignored).
# Chrome must be installed. Does not run in PR CI unless LHCI_URL is set.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BASE_URL="${LHCI_URL:-${BASE_URL:-http://localhost:3000}}"
mkdir -p "$REPORT_DIR"

if ! command -v npx >/dev/null 2>&1; then
  echo "npx is required" >&2
  exit 1
fi

paths=("/" "/players" "/matches")
for path in "${paths[@]}"; do
  slug="$(echo "$path" | tr '/' '_' | sed 's/^_//')"
  [[ -z "$slug" ]] && slug="home"
  out="$REPORT_DIR/lighthouse-${slug}-${STAMP}.json"
  echo "lighthouse ${BASE_URL}${path}"
  npx --yes lighthouse "${BASE_URL}${path}" \
    --only-categories=performance \
    --chrome-flags="--headless --no-sandbox" \
    --output=json \
    --output-path="$out" \
    --quiet || true
done

echo "reports in $REPORT_DIR"
