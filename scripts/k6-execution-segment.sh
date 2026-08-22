#!/bin/sh
set -eu

INDEX="${K6_WORKER_INDEX:-${JOB_COMPLETION_INDEX:-0}}"
WORKERS="${K6_WORKERS:-1}"
SCRIPT="${1:-/scripts/match-websocket.js}"
if [ "$#" -ge 1 ]; then
  shift
fi

case "$INDEX$WORKERS" in
  *[!0-9]*)
    echo "K6_WORKER_INDEX and K6_WORKERS must be integers" >&2
    exit 1
    ;;
esac
if [ "$WORKERS" -lt 1 ] || [ "$INDEX" -ge "$WORKERS" ]; then
  echo "K6_WORKER_INDEX must be < K6_WORKERS" >&2
  exit 1
fi

sequence="0"
i=1
while [ "$i" -lt "$WORKERS" ]; do
  sequence="${sequence},${i}/${WORKERS}"
  i=$((i + 1))
done
sequence="${sequence},1"

next=$((INDEX + 1))
SUMMARY="${SUMMARY_EXPORT:-/results/worker-${INDEX}.json}"
OUT_ARGS=""
if [ -n "${K6_PROMETHEUS_RW_SERVER_URL:-}" ]; then
  OUT_ARGS="-o experimental-prometheus-rw"
fi

mkdir -p "$(dirname "$SUMMARY")"
# Native histograms are the only honest global p99 across workers; per-pod summaries cannot be merged.
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"
exec k6 run \
  --execution-segment "${INDEX}/${WORKERS}:${next}/${WORKERS}" \
  --execution-segment-sequence "$sequence" \
  --summary-export "$SUMMARY" \
  $OUT_ARGS \
  "$@" \
  "$SCRIPT"
