#!/usr/bin/env bash
# In-memory million-event archive processor. Not HTTP, not Postgres, not Elasticsearch.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JMH_INCLUDE="${JMH_INCLUDE:-ArchiveThroughputBenchmark}"
export JMH_HEAP="${JMH_HEAP:-1024m}"
if [[ "${JMH_EVIDENCE:-false}" == "true" ]]; then
  export JMH_FORKS="${JMH_FORKS:-3}"
else
  export JMH_FORKS="${JMH_FORKS:-1}"
fi
OUT="${JMH_OUTPUT:-$ROOT/services/performance-benchmarks/target/archive-tape-jmh.json}"
export JMH_OUTPUT="$OUT"

echo "operation=in_memory_archive_events"
echo "claim=deterministic tape processing of generated events, not durable commits"
"$ROOT/scripts/jmh-run.sh"

python3 - "$OUT" <<'PY'
import json, sys
path = sys.argv[1]
data = json.load(open(path, encoding="utf-8"))
print("workers tape/s events/s")
for row in data:
    params = row.get("params") or {}
    workers = params.get("workers", "?")
    score = row["primaryMetric"]["score"]
    print(f"{workers} {score:.3f} {score * 1_000_000:.0f}")
PY
