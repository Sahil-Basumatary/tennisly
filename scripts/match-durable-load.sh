#!/usr/bin/env bash
# Local-only HTTP -> Postgres commit benchmark. It refuses non-loopback targets.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
SERVER_PORT="${MATCH_PERF_PORT:-18094}"
BASE_URL="http://localhost:${SERVER_PORT}"
POSTGRES_PORT="${POSTGRES_PORT:-15432}"
POSTGRES_USER="${POSTGRES_USER:-tennisly}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-tennisly_dev}"
POSTGRES_DB="${POSTGRES_DB_MATCHES:-tennisly_matches}"
REDIS_PORT="${REDIS_PORT:-16379}"
WRITE_VUS="${WRITE_VUS:-8}"
WARMUP_VUS="${WARMUP_VUS:-2}"
WRITE_DURATION="${WRITE_DURATION:-30s}"
JAR="$ROOT/services/match-service/target/match-service-0.0.1-SNAPSHOT.jar"
SUMMARY="$REPORT_DIR/k6-durable-write-${RUN_ID}.json"
OUTPUT="$REPORT_DIR/k6-durable-write-${RUN_ID}.txt"
SERVICE_LOG="$REPORT_DIR/match-durable-service-${RUN_ID}.log"
SERVICE_PID=""

if [[ ! "$RUN_ID" =~ ^[A-Za-z0-9_-]+$ ]]; then
  echo "RUN_ID must contain only letters, numbers, underscore or hyphen" >&2
  exit 1
fi
for command in curl k6 nc psql python3; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "$command is required" >&2
    exit 1
  fi
done
if nc -z localhost "$SERVER_PORT" >/dev/null 2>&1; then
  echo "port $SERVER_PORT is already in use; refusing to benchmark an unknown service" >&2
  exit 1
fi
if ! PGPASSWORD="$POSTGRES_PASSWORD" psql \
  -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -Atqc "select 1" >/dev/null; then
  echo "local Postgres is unavailable on port $POSTGRES_PORT" >&2
  exit 1
fi
if [[ "$(printf '*1\r\n$4\r\nPING\r\n' | nc -w 2 localhost "$REDIS_PORT" || true)" != *PONG* ]]; then
  echo "local Redis is unavailable on port $REDIS_PORT" >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

stop_service() {
  if [[ -n "$SERVICE_PID" ]] && kill -0 "$SERVICE_PID" >/dev/null 2>&1; then
    kill "$SERVICE_PID"
    wait "$SERVICE_PID" 2>/dev/null || true
  fi
  SERVICE_PID=""
}
trap stop_service EXIT

mkdir -p "$REPORT_DIR"
cd "$ROOT"
./mvnw -pl services/match-service -am package -DskipTests -Djacoco.skip=true -q

env \
  SERVER_PORT="$SERVER_PORT" \
  POSTGRES_HOST=localhost \
  POSTGRES_PORT="$POSTGRES_PORT" \
  POSTGRES_USER="$POSTGRES_USER" \
  POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  POSTGRES_DB_MATCHES="$POSTGRES_DB" \
  REDIS_HOST=localhost \
  REDIS_PORT="$REDIS_PORT" \
  TENNISLY_KAFKA_ENABLED=false \
  MATCH_INGEST_ENABLED=false \
  EUREKA_CLIENT_ENABLED=false \
  SPRING_CLOUD_CONFIG_ENABLED=false \
  MANAGEMENT_HEALTH_KAFKA_ENABLED=false \
  GATEWAY_INTERNAL_TOKEN="${GATEWAY_INTERNAL_TOKEN:-}" \
  POSTGRES_POOL_MAX="${POSTGRES_POOL_MAX:-16}" \
  LOGGING_LEVEL_DEV_SAHILBASUMATARY_MATCHSERVICE=ERROR \
  java -jar "$JAR" >"$SERVICE_LOG" 2>&1 &
SERVICE_PID=$!

for _ in $(seq 1 90); do
  if curl -fsS "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$SERVICE_PID" >/dev/null 2>&1; then
    echo "match-service exited before becoming healthy; see $SERVICE_LOG" >&2
    exit 1
  fi
  sleep 1
done
if ! curl -fsS "$BASE_URL/actuator/health" >/dev/null; then
  echo "match-service did not become healthy; see $SERVICE_LOG" >&2
  exit 1
fi

echo "base=$BASE_URL run_id=$RUN_ID vus=$WRITE_VUS duration=$WRITE_DURATION"
set +e
k6 run \
  --summary-export "$SUMMARY" \
  -e BASE_URL="$BASE_URL" \
  -e RUN_ID="$RUN_ID" \
  -e WRITE_VUS="$WRITE_VUS" \
  -e WARMUP_VUS="$WARMUP_VUS" \
  -e WRITE_DURATION="$WRITE_DURATION" \
  -e GATEWAY_INTERNAL_TOKEN="${GATEWAY_INTERNAL_TOKEN:-}" \
  "$ROOT/tests/load/match-durable-write.js" | tee "$OUTPUT"
K6_STATUS=${PIPESTATUS[0]}
set -e

stop_service

COUNTS="$(
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v prefix="perf-${RUN_ID}-%" -Atq <<'SQL'
WITH perf_matches AS (
  SELECT id, point_count, live_sequence FROM matches WHERE external_id LIKE :'prefix'
)
SELECT
  COUNT(*),
  COALESCE(SUM(point_count), 0),
  (SELECT COUNT(*) FROM match_points WHERE match_id IN (SELECT id FROM perf_matches)),
  (SELECT COUNT(*) FROM match_event_logs
    WHERE match_id IN (SELECT id FROM perf_matches) AND event_type = 'POINT_RECORDED'),
  (SELECT COUNT(*) FROM match_outbox
    WHERE event_json->>'matchId' IN (SELECT id::text FROM perf_matches)
      AND event_json->>'eventType' = 'MATCH_POINT_RECORDED'),
  (SELECT COUNT(*) FROM perf_matches AS match
    WHERE match.live_sequence <> (
      SELECT COUNT(*) FROM match_event_logs AS event_log
      WHERE event_log.match_id = match.id
    ))
FROM perf_matches;
SQL
)"
IFS='|' read -r MATCHES POINT_COUNT POINT_ROWS EVENT_ROWS OUTBOX_ROWS SEQUENCE_VIOLATIONS <<<"$COUNTS"
echo "durability matches=$MATCHES point_count=$POINT_COUNT points=$POINT_ROWS events=$EVENT_ROWS outbox=$OUTBOX_ROWS sequence_violations=$SEQUENCE_VIOLATIONS"

INTEGRITY_STATUS=0
if [[ "$MATCHES" -ne $((2 * (WRITE_VUS + WARMUP_VUS))) ]] \
  || [[ "$POINT_COUNT" -ne "$POINT_ROWS" ]] \
  || [[ "$POINT_ROWS" -ne "$EVENT_ROWS" ]] \
  || [[ "$POINT_ROWS" -ne "$OUTBOX_ROWS" ]] \
  || [[ "$SEQUENCE_VIOLATIONS" -ne 0 ]]; then
  echo "durability invariant failed" >&2
  INTEGRITY_STATUS=1
fi

python3 - "$SUMMARY" "$WRITE_DURATION" "$REPORT_DIR/durable-${RUN_ID}.json" "${PERF_PHASE:-warm}" <<'PY'
import json
import re
import sys

data = json.load(open(sys.argv[1], encoding="utf-8"))
duration_match = re.fullmatch(r"([0-9]+(?:\.[0-9]+)?)(ms|s|m|h)", sys.argv[2])
if not duration_match:
    raise SystemExit(f"unsupported WRITE_DURATION: {sys.argv[2]}")
amount = float(duration_match.group(1))
duration_seconds = amount * {"ms": 0.001, "s": 1, "m": 60, "h": 3600}[duration_match.group(2)]
metrics = data["metrics"]
duration = metrics["durable_point_commit_ms"]
committed = metrics["durable_points_committed"]
errors = metrics["durable_point_non_201"]
rate = committed["count"] / duration_seconds
print(
    "claim=atomic commit TPS "
    f"commits={committed['count']} stage_tps={rate:.2f} "
    f"p50={duration['med']:.2f}ms p95={duration['p(95)']:.2f}ms "
    f"p99={duration['p(99)']:.2f}ms errors={errors['value']:.6f}"
)
payload = {
    "phase": sys.argv[4],
    "operations": [
        {
            "operation": "atomic_commit_tps",
            "rate": rate,
            "p50_ms": duration["med"],
            "p95_ms": duration["p(95)"],
            "p99_ms": duration["p(99)"],
        }
    ],
}
json.dump(payload, open(sys.argv[3], "w", encoding="utf-8"), indent=2)
PY

if [[ "${KEEP_PERF_DATA:-false}" != "true" ]]; then
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v prefix="perf-${RUN_ID}-%" -v ON_ERROR_STOP=1 -q <<'SQL'
CREATE TEMP TABLE perf_match_ids AS
  SELECT id FROM matches WHERE external_id LIKE :'prefix';
DELETE FROM match_outbox
  WHERE event_json->>'matchId' IN (SELECT id::text FROM perf_match_ids);
DELETE FROM matches WHERE id IN (SELECT id FROM perf_match_ids);
SQL
  echo "cleaned benchmark rows"
fi

echo "summary=$SUMMARY"
echo "service_log=$SERVICE_LOG"
if [[ "$INTEGRITY_STATUS" -ne 0 ]]; then
  exit "$INTEGRITY_STATUS"
fi
if [[ "$K6_STATUS" -ne 0 && "${PERF_RECORD_ONLY:-false}" == "true" ]]; then
  echo "k6 thresholds missed; durability held so evidence records the p99 instead of aborting"
  exit 0
fi
exit "$K6_STATUS"
