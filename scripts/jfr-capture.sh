#!/usr/bin/env bash
# Capture a short JFR on a local JVM during a load run. Never expose this on Render.
#
#   JFR_PID=$(pgrep -f match-service) DURATION=30 ./scripts/jfr-capture.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DURATION="${DURATION:-30}"
JFR_PID="${JFR_PID:-}"

mkdir -p "$REPORT_DIR"
OUT="$REPORT_DIR/jfr-${STAMP}.jfr"

if [[ -z "$JFR_PID" ]]; then
  echo "set JFR_PID to a local JVM pid" >&2
  exit 1
fi

jcmd "$JFR_PID" JFR.start duration="${DURATION}s" filename="$OUT" settings=profile
echo "recording $DURATION s to $OUT"
sleep "$DURATION"
echo "wrote $OUT"
