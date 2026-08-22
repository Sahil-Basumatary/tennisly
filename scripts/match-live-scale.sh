#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
SUMMARY="$REPORT_DIR/live-scale-${STAMP}.txt"
PROFILE="${PROFILE:-realistic}"
APPROVE_SCALE_PROVISION="${APPROVE_SCALE_PROVISION:-false}"
STATUS=0
CEILING=""
MODE=""

mkdir -p "$REPORT_DIR"
: >"$SUMMARY"

log() {
  echo "$1" | tee -a "$SUMMARY"
}

detect_mode() {
  if [[ "${FORCE_MODE:-}" == "local" || "${FORCE_MODE:-}" == "k8s" ]]; then
    MODE="$FORCE_MODE"
    return
  fi
  if command -v kubectl >/dev/null 2>&1 && kubectl cluster-info >/dev/null 2>&1; then
    MODE=k8s
  else
    MODE=local
  fi
}

workers_for() {
  local clients="$1"
  if [[ "$clients" -ge 10000 ]]; then
    echo 10
  elif [[ "$clients" -ge 1000 ]]; then
    echo 2
  else
    echo 1
  fi
}

ramp_for() {
  local clients="$1"
  if [[ "$clients" -ge 100000 ]]; then
    echo "10m"
  elif [[ "$clients" -ge 10000 ]]; then
    echo "2m"
  elif [[ "$clients" -ge 1000 ]]; then
    echo "20s"
  else
    echo "5s"
  fi
}

hold_ms_for() {
  local clients="$1"
  if [[ "$clients" -ge 100000 ]]; then
    echo 600000
  elif [[ "$clients" -ge 10000 ]]; then
    echo 120000
  else
    echo 15000
  fi
}

run_local_stage() {
  local clients="$1"
  local ramp hold
  ramp="$(ramp_for "$clients")"
  hold="$(hold_ms_for "$clients")"
  if [[ "$clients" -gt 10000 && "${ALLOW_LARGE_LOCAL_RUN:-false}" != "true" ]]; then
    log "stage ${clients} SKIP local cap 10000 (set ALLOW_LARGE_LOCAL_RUN=true to override)"
    return 2
  fi
  log "=== local $PROFILE stage ${clients} ramp=${ramp} hold_ms=${hold} ==="
  if env \
    RUN_ID="scale-${STAMP}-${clients}" \
    WS_MODE="$PROFILE" \
    WS_CLIENTS="$clients" \
    MATCH_INSTANCE_COUNT="${MATCH_INSTANCE_COUNT:-1}" \
    SUBSCRIBER_RAMP="$ramp" \
    WRITER_START="$ramp" \
    WS_HOLD_MS="$hold" \
    WS_DURATION="$([[ "$clients" -ge 10000 ]] && echo 2m || echo 15s)" \
    SUBSCRIBER_MAX_DURATION=20m \
    POINT_INTERVAL_MS="$([[ "$PROFILE" == hot ]] && echo 100 || echo 250)" \
    WARMUP_POINTS=5 \
    DELIVERY_P99_MS=50 \
    REQUIRE_FULL_CONNECT=true \
    REPLAY_ON_RECONNECT=true \
    "$ROOT/scripts/match-websocket-load.sh" | tee -a "$SUMMARY"; then
    log "stage ${clients} PASS"
    return 0
  fi
  log "stage ${clients} FAIL"
  return 1
}

ensure_k8s_ready() {
  if [[ "$APPROVE_SCALE_PROVISION" != "true" ]]; then
    log "k8s provisioning blocked: set APPROVE_SCALE_PROVISION=true after reviewing tests/load/baselines/2026-08-22-live-100k-preflight.md"
    return 3
  fi
  if [[ -z "${MATCH_SERVICE_IMAGE:-}" || "$MATCH_SERVICE_IMAGE" == MATCH_SERVICE_IMAGE ]]; then
    log "MATCH_SERVICE_IMAGE must be an immutable image digest"
    return 3
  fi
  if ! kubectl -n tennisly-live get secret tennisly-live-db >/dev/null 2>&1; then
    log "missing secret tennisly-live/tennisly-live-db"
    return 3
  fi
  kubectl apply -f "$ROOT/infrastructure/kubernetes/live-100k/namespace.yaml"
  kubectl apply -f "$ROOT/infrastructure/kubernetes/live-100k/redis.yaml"
  kubectl apply -f "$ROOT/infrastructure/kubernetes/live-100k/prometheus.yaml"
  sed "s|MATCH_SERVICE_IMAGE|${MATCH_SERVICE_IMAGE}|g" \
    "$ROOT/infrastructure/kubernetes/live-100k/match-service.yaml" | kubectl apply -f -
  kubectl -n tennisly-live create configmap k6-live-scripts \
    --from-file=match-websocket.js="$ROOT/tests/load/match-websocket.js" \
    --from-file=k6-execution-segment.sh="$ROOT/scripts/k6-execution-segment.sh" \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl -n tennisly-live rollout status deployment/match-service --timeout=180s
  kubectl -n tennisly-live rollout status deployment/prometheus --timeout=120s
}

run_k8s_stage() {
  local clients="$1"
  local workers ramp hold job_file job_name run_id rendered
  workers="$(workers_for "$clients")"
  ramp="$(ramp_for "$clients")"
  hold="$(hold_ms_for "$clients")"
  run_id="scale-${STAMP}-${PROFILE}-${clients}"
  if [[ "$PROFILE" == hot ]]; then
    job_file="$ROOT/infrastructure/kubernetes/live-100k/k6-hot.yaml"
    job_name=k6-ws-hot
  else
    job_file="$ROOT/infrastructure/kubernetes/live-100k/k6-realistic.yaml"
    job_name=k6-ws-realistic
  fi
  kubectl -n tennisly-live delete job "$job_name" --ignore-not-found
  rendered="$REPORT_DIR/${job_name}-${run_id}.yaml"
  python3 - "$job_file" "$rendered" "$clients" "$workers" "$ramp" "$hold" "$run_id" "$PROFILE" <<'PY'
import sys
from pathlib import Path
src, dest, clients, workers, ramp, hold, run_id, profile = sys.argv[1:9]
text = Path(src).read_text(encoding="utf-8")
replacements = {
    "k6-ws-realistic" if profile != "hot" else "k6-ws-hot": f"k6-ws-{profile}-{clients}",
    'value: "10"': f'value: "{workers}"',
    "completions: 10": f"completions: {workers}",
    "parallelism: 10": f"parallelism: {workers}",
    'value: "100000"': f'value: "{clients}"',
    'value: "10m"': f'value: "{ramp}"',
    'value: "600000"': f'value: "{hold}"',
    'value: "k8s-scale-realistic"': f'value: "{run_id}"',
    'value: "k8s-scale-hot"': f'value: "{run_id}"',
}
# First replacement of K6_WORKERS 10 is also "10" for completions; apply targeted ones already.
text = text.replace("completions: 10", f"completions: {workers}")
text = text.replace("parallelism: 10", f"parallelism: {workers}")
text = text.replace('value: "100000"', f'value: "{clients}"')
text = text.replace('value: "600000"', f'value: "{hold}"')
text = text.replace('value: "k8s-scale-realistic"', f'value: "{run_id}"')
text = text.replace('value: "k8s-scale-hot"', f'value: "{run_id}"')
# Keep writer start aligned with ramp; both default 10m in the template.
text = text.replace('name: SUBSCRIBER_RAMP\n              value: "10m"', f'name: SUBSCRIBER_RAMP\n              value: "{ramp}"')
text = text.replace('name: WRITER_START\n              value: "10m"', f'name: WRITER_START\n              value: "{ramp}"')
text = text.replace('name: K6_WORKERS\n              value: "10"', f'name: K6_WORKERS\n              value: "{workers}"')
if int(clients) < 10000:
    text = text.replace("memory: 16Gi", "memory: 2Gi")
    text = text.replace("memory: 24Gi", "memory: 4Gi")
Path(dest).write_text(text, encoding="utf-8")
PY
  log "=== k8s $PROFILE stage ${clients} workers=${workers} ramp=${ramp} ==="
  kubectl apply -f "$rendered"
  if ! kubectl -n tennisly-live wait --for=condition=complete "job/${job_name}" --timeout=2400s; then
    log "stage ${clients} FAIL job did not complete"
    return 1
  fi
  log "stage ${clients} PASS job completed; fetch Prometheus histograms before publishing a 100k claim"
  return 0
}

detect_mode
log "mode=$MODE profile=$PROFILE stamp=$STAMP"
if [[ "$MODE" == k8s ]]; then
  if ! ensure_k8s_ready; then
    STATUS=3
    CEILING="blocked-no-provision-approval"
    log "capacity-ceiling=$CEILING"
    log "scale-summary=$SUMMARY status=$STATUS"
    exit "$STATUS"
  fi
fi

STAGES=(100 1000 10000 25000 50000 100000)
for clients in "${STAGES[@]}"; do
  if [[ "$MODE" == local ]]; then
    if ! run_local_stage "$clients"; then
      code=$?
      STATUS=1
      CEILING="$clients"
      if [[ "$code" -eq 2 && "$clients" -gt 10000 ]]; then
        CEILING="local-cap-10000"
      fi
      break
    fi
    CEILING="passed-${clients}"
  else
    if ! run_k8s_stage "$clients"; then
      STATUS=1
      CEILING="$clients"
      break
    fi
    CEILING="passed-${clients}"
  fi
done

log "capacity-ceiling=$CEILING"
log "scale-summary=$SUMMARY status=$STATUS"
exit "$STATUS"
