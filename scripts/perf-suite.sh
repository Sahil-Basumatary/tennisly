#!/usr/bin/env bash
# Unified local evidence runner. Labels the five claim types separately.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAMP="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
SESSION="${PERF_SESSION_DIR:-$ROOT/.run/performance/session-$STAMP}"
REPORT_DIR="$SESSION"
export PERF_REPORT_DIR="$SESSION"
export JMH_EVIDENCE="${JMH_EVIDENCE:-true}"
export JMH_FORKS="${JMH_FORKS:-3}"
export GATEWAY_INTERNAL_TOKEN="${GATEWAY_INTERNAL_TOKEN:-perf-local-evidence-token}"
export PERF_RECORD_ONLY="${PERF_RECORD_ONLY:-true}"
COLD_WARM_RUNS="${SERVICE_EVIDENCE_RUNS:-6}"
SKIP_HTTP="${PERF_SKIP_HTTP:-false}"
SKIP_JMH="${PERF_SKIP_JMH:-false}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

mkdir -p "$SESSION"
cd "$ROOT"

GIT_SHA="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
HW="$(uname -s -m); $(sysctl -n machdep.cpu.brand_string 2>/dev/null || true); $(sysctl -n hw.ncpu 2>/dev/null || nproc) cores"
JVM="$(java -version 2>&1 | tr '\n' ' ')"

POSTGRES_NOTE="not started"
if [[ "${PERF_EPHEMERAL_POSTGRES:-true}" == "true" && "$SKIP_HTTP" != "true" ]]; then
  POSTGRES_NOTE="$("$ROOT/scripts/perf-postgres.sh")"
  export POSTGRES_PORT="${PERF_POSTGRES_PORT:-15442}"
fi

export STAMP
python3 - "$SESSION/session.json" <<PY
import json, os, sys
path = sys.argv[1]
payload = {
  "schema": "tennisly.perf.session.v1",
  "title": "Credible local performance evidence",
  "date": os.environ.get("STAMP") or """$STAMP""",
  "gitSha": """$GIT_SHA""",
  "hardware": """$HW""",
  "jvm": """$JVM""",
  "postgres": """$POSTGRES_NOTE""".splitlines()[0] if """$POSTGRES_NOTE""" else "",
  "jmhForks": int(os.environ.get("JMH_FORKS", "3")),
  "claimLabels": [
    "in-process latency",
    "replay frames/s",
    "atomic commit TPS",
    "transactional batch points/s",
    "staging/promote rows/s",
  ],
  "limitations": "Local laptop only. Absolute floors apply to this suite, not PR CI. Sub-1ms is in-process CPU, not HTTP.",
}
json.dump(payload, open(path, "w", encoding="utf-8"), indent=2)
print("session", path)
PY

if [[ "$SKIP_JMH" != "true" ]]; then
  echo "claim=in-process latency"
  JMH_INCLUDE="HotPathBenchmark|PointDecisionThroughputBenchmark|PointCommitCpuBenchmark" \
    JMH_OUTPUT="$SESSION/jmh-hotpath.json" \
    JMH_HEAP="${JMH_HEAP:-256m}" \
    "$ROOT/scripts/jmh-run.sh"
  echo "claim=replay frames/s"
  JMH_OUTPUT="$SESSION/jmh-replay.json" "$ROOT/scripts/replay-physics-bench.sh"
  echo "claim=in-memory archive events (not staging/promote rows/s)"
  JMH_OUTPUT="$SESSION/jmh-archive.json" "$ROOT/scripts/archive-tape-bench.sh"
fi

if [[ "$SKIP_HTTP" != "true" ]]; then
  for index in $(seq 1 "$COLD_WARM_RUNS"); do
    if [[ "$index" -eq 1 ]]; then
      phase=cold
    else
      phase=warm
    fi
    echo "claim=atomic commit TPS phase=$phase run=$index"
    RUN_ID="${STAMP}-durable-${phase}-${index}" \
      PERF_PHASE="$phase" \
      MATCH_PERF_PORT="$((18094 + index))" \
      "$ROOT/scripts/match-durable-load.sh"
    echo "claim=transactional batch points/s and staging/promote rows/s phase=$phase run=$index"
    RUN_ID="${STAMP}-bulk-${phase}-${index}" \
      PERF_PHASE="$phase" \
      MATCH_PERF_PORT="$((18110 + index))" \
      "$ROOT/scripts/match-bulk-ingest.sh"
  done
fi

python3 "$ROOT/scripts/perf-report.py" "$SESSION" "$SESSION/SUMMARY.md"
echo "evidence_session=$SESSION"
echo "summary=$SESSION/SUMMARY.md"
echo "Copy SUMMARY.md into tests/load/baselines/ after review. Raw JFR/k6/JMH stays under .run/performance/."
