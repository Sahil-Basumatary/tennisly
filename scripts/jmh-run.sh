#!/usr/bin/env bash
# Forked JMH via the shaded uber-jar. Do not use exec:java — ForkedMain is missing there.
#
#   ./scripts/jmh-run.sh
#   JMH_QUICK=true ./scripts/jmh-run.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODULE="$ROOT/services/performance-benchmarks"
JAR="$MODULE/target/benchmarks.jar"
OUT="${JMH_OUTPUT:-$MODULE/target/jmh-result.json}"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

cd "$ROOT"
./mvnw -pl services/performance-benchmarks -am package -DskipTests -q

if [[ ! -f "$JAR" ]]; then
  echo "missing $JAR" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT")" "$REPORT_DIR"
echo "jar=$JAR"
echo "json=$OUT"
java -Djmh.output="$OUT" -jar "$JAR"
cp "$OUT" "$REPORT_DIR/jmh-${STAMP}.json"
echo "wrote $REPORT_DIR/jmh-${STAMP}.json"
