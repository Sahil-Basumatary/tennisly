#!/usr/bin/env bash
# In-process replay physics JMH. Reports assembler-only and full solver-to-frame separately.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JMH_INCLUDE="${JMH_INCLUDE:-ReplayPhysicsBenchmark}"
export JMH_HEAP="${JMH_HEAP:-512m}"
if [[ "${JMH_EVIDENCE:-false}" == "true" ]]; then
  export JMH_FORKS="${JMH_FORKS:-3}"
else
  export JMH_FORKS="${JMH_FORKS:-1}"
fi
OUT="${JMH_OUTPUT:-$ROOT/services/performance-benchmarks/target/replay-physics-jmh.json}"
export JMH_OUTPUT="$OUT"
GOLDEN="${REPLAY_GOLDEN:-$ROOT/tests/load/baselines/replay-golden-sha256.txt}"
read -r POINT_FRAMES MATCH_FRAMES < <(python3 "$ROOT/scripts/replay-fixture-counts.py" "$GOLDEN")

echo "operation=generated_replay_frames"
echo "claim=replay frames/s"
echo "fixture_point_frames=$POINT_FRAMES fixture_match_frames=$MATCH_FRAMES"
"$ROOT/scripts/jmh-run.sh"

python3 - "$OUT" "$POINT_FRAMES" "$MATCH_FRAMES" <<'PY'
import json, sys
path, point_frames, match_frames = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
data = json.load(open(path, encoding="utf-8"))
print("operation_name mode throughput_ops_s derived")
for row in data:
    name = row["benchmark"].rsplit(".", 1)[-1]
    mode = row.get("mode", "")
    score = row["primaryMetric"]["score"]
    unit = row["primaryMetric"]["scoreUnit"]
    derived = ""
    if mode == "thrpt" and name == "assemblerOnly":
        derived = f" assembler_only_frames/s={score * point_frames:.1f}"
    elif mode == "thrpt" and name == "fullPointPipeline":
        derived = f" full_pipeline_frames/s={score * point_frames:.1f}"
    elif mode == "thrpt" and name == "fullMatchPipeline":
        derived = f" full_pipeline_frames/s={score * match_frames:.1f}"
    print(f"{name} {mode} {score:.3f} {unit}{derived}")
PY
