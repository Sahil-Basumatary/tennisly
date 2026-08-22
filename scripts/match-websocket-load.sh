#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
SERVER_PORT="${MATCH_WS_PERF_PORT:-18094}"
BASE_URL="http://localhost:${SERVER_PORT}"
WS_URL="ws://localhost:${SERVER_PORT}/ws/matches"
POSTGRES_PORT="${POSTGRES_PORT:-15432}"
POSTGRES_USER="${POSTGRES_USER:-tennisly}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-tennisly_dev}"
POSTGRES_DB="${POSTGRES_DB_MATCHES:-tennisly_matches}"
REDIS_PORT="${REDIS_PORT:-16379}"
WS_MODE="${WS_MODE:-realistic}"
WS_CLIENTS="${WS_CLIENTS:-100}"
WS_DURATION="${WS_DURATION:-20s}"
WRITER_START="${WRITER_START:-3s}"
POINT_INTERVAL_MS="${POINT_INTERVAL_MS:-250}"
WARMUP_POINTS="${WARMUP_POINTS:-5}"
DELIVERY_P99_MS="${DELIVERY_P99_MS:-50}"
SUBSCRIBER_ITERATIONS="${SUBSCRIBER_ITERATIONS:-1}"
WS_HOLD_MS="${WS_HOLD_MS:-25000}"
SUBSCRIBER_MAX_DURATION="${SUBSCRIBER_MAX_DURATION:-30s}"
SLOW_CLIENT_PERCENT="${SLOW_CLIENT_PERCENT:-0}"
SLOW_CLIENT_DELAY_MS="${SLOW_CLIENT_DELAY_MS:-100}"
JAR="$ROOT/services/match-service/target/match-service-0.0.1-SNAPSHOT.jar"
SUMMARY="$REPORT_DIR/k6-match-ws-${RUN_ID}.json"
OUTPUT="$REPORT_DIR/k6-match-ws-${RUN_ID}.txt"
SERVICE_LOG="$REPORT_DIR/match-ws-service-${RUN_ID}.log"
SERVER_METRIC="$REPORT_DIR/match-ws-server-metric-${RUN_ID}.json"
SERVICE_PID=""

case "$WS_MODE" in
  hot)
    MATCH_COUNT=1
    ;;
  realistic)
    MATCH_COUNT="${MATCH_COUNT:-8}"
    ;;
  *)
    echo "WS_MODE must be realistic or hot" >&2
    exit 1
    ;;
esac

if [[ ! "$RUN_ID" =~ ^[A-Za-z0-9_-]+$ ]]; then
  echo "RUN_ID must contain only letters, numbers, underscore or hyphen" >&2
  exit 1
fi
for value in "$WS_CLIENTS" "$MATCH_COUNT" "$POINT_INTERVAL_MS" "$WARMUP_POINTS" "$DELIVERY_P99_MS" \
  "$SUBSCRIBER_ITERATIONS" "$WS_HOLD_MS" "$SLOW_CLIENT_PERCENT" "$SLOW_CLIENT_DELAY_MS"; do
  if [[ ! "$value" =~ ^[0-9]+$ ]]; then
    echo "WebSocket load numeric settings must be non-negative integers" >&2
    exit 1
  fi
done
if [[ "$WS_CLIENTS" -lt 1 || "$MATCH_COUNT" -lt 1 || "$SUBSCRIBER_ITERATIONS" -lt 1 ]]; then
  echo "WS_CLIENTS, MATCH_COUNT and SUBSCRIBER_ITERATIONS must be positive" >&2
  exit 1
fi
if [[ "$WS_CLIENTS" -gt 10000 && "${ALLOW_LARGE_LOCAL_RUN:-false}" != "true" ]]; then
  echo "Refusing more than 10,000 local clients; use distributed workers for 100k" >&2
  exit 1
fi
for command in curl k6 nc psql python3; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "$command is required" >&2
    exit 1
  fi
done
if nc -z localhost "$SERVER_PORT" >/dev/null 2>&1; then
  echo "port $SERVER_PORT is already in use; refusing to test an unknown service" >&2
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
  SPRING_PROFILES_ACTIVE=perf \
  POSTGRES_HOST=localhost \
  POSTGRES_PORT="$POSTGRES_PORT" \
  POSTGRES_USER="$POSTGRES_USER" \
  POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  POSTGRES_DB_MATCHES="$POSTGRES_DB" \
  REDIS_HOST=localhost \
  REDIS_PORT="$REDIS_PORT" \
  MATCH_SERVICE_WS_ALLOWED_ORIGINS="$BASE_URL" \
  TENNISLY_KAFKA_ENABLED=false \
  MATCH_INGEST_ENABLED=false \
  EUREKA_CLIENT_ENABLED=false \
  SPRING_CLOUD_CONFIG_ENABLED=false \
  MANAGEMENT_HEALTH_KAFKA_ENABLED=false \
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

echo "base=$BASE_URL mode=$WS_MODE clients=$WS_CLIENTS matches=$MATCH_COUNT duration=$WS_DURATION"
set +e
k6 run \
  --summary-export "$SUMMARY" \
  -e BASE_URL="$BASE_URL" \
  -e WS_URL="$WS_URL" \
  -e RUN_ID="$RUN_ID" \
  -e WS_CLIENTS="$WS_CLIENTS" \
  -e MATCH_COUNT="$MATCH_COUNT" \
  -e WS_DURATION="$WS_DURATION" \
  -e WRITER_START="$WRITER_START" \
  -e POINT_INTERVAL_MS="$POINT_INTERVAL_MS" \
  -e WARMUP_POINTS="$WARMUP_POINTS" \
  -e DELIVERY_P99_MS="$DELIVERY_P99_MS" \
  -e SUBSCRIBER_ITERATIONS="$SUBSCRIBER_ITERATIONS" \
  -e WS_HOLD_MS="$WS_HOLD_MS" \
  -e SUBSCRIBER_MAX_DURATION="$SUBSCRIBER_MAX_DURATION" \
  -e SLOW_CLIENT_PERCENT="$SLOW_CLIENT_PERCENT" \
  -e SLOW_CLIENT_DELAY_MS="$SLOW_CLIENT_DELAY_MS" \
  "$ROOT/tests/load/match-websocket.js" | tee "$OUTPUT"
K6_STATUS=${PIPESTATUS[0]}
set -e

curl -fsS \
  "$BASE_URL/actuator/metrics/match.live_publish_after_commit" \
  >"$SERVER_METRIC" || printf '{}\n' >"$SERVER_METRIC"
stop_service

COUNTS="$(
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v prefix="ws-perf-${RUN_ID}-%" -Atq <<'SQL'
WITH perf_matches AS (
  SELECT id, live_sequence FROM matches WHERE external_id LIKE :'prefix'
)
SELECT
  COUNT(*),
  (SELECT COUNT(*) FROM match_event_logs
    WHERE match_id IN (SELECT id FROM perf_matches)),
  (SELECT COUNT(*) FROM match_outbox
    WHERE event_json->>'matchId' IN (SELECT id::text FROM perf_matches)),
  (SELECT COUNT(*) FROM perf_matches AS match
    WHERE match.live_sequence <> (
      SELECT COUNT(*) FROM match_event_logs AS event_log
      WHERE event_log.match_id = match.id
    ) OR EXISTS (
      SELECT 1 FROM match_event_logs AS event_log
      WHERE event_log.match_id = match.id
        AND (event_log.sequence_number < 1
          OR event_log.sequence_number > match.live_sequence)
    ))
FROM perf_matches;
SQL
)"
IFS='|' read -r MATCHES EVENT_ROWS OUTBOX_ROWS SEQUENCE_VIOLATIONS <<<"$COUNTS"
echo "integrity matches=$MATCHES events=$EVENT_ROWS outbox=$OUTBOX_ROWS sequence_violations=$SEQUENCE_VIOLATIONS"

INTEGRITY_STATUS=0
if [[ "$MATCHES" -ne "$MATCH_COUNT" ]] \
  || [[ "$EVENT_ROWS" -ne "$OUTBOX_ROWS" ]] \
  || [[ "$SEQUENCE_VIOLATIONS" -ne 0 ]]; then
  echo "WebSocket benchmark durability invariant failed" >&2
  INTEGRITY_STATUS=1
fi

python3 - "$SUMMARY" "$SERVER_METRIC" <<'PY'
import json
import sys

summary = json.load(open(sys.argv[1], encoding="utf-8"))
metrics = summary.get("metrics", {})

def metric(name):
    return metrics.get(name, {})

delivery = metric("live_ws_delivery_ms")
connect = metric("live_ws_connect_ms")
connected = metric("live_ws_connected")
gaps = metric("live_ws_sequence_gaps")
duplicates = metric("live_ws_duplicates")
messages = metric("live_ws_messages")
print(
    "client "
    f"messages={messages.get('count', 0):.0f} "
    f"connected={connected.get('value', connected.get('rate', 0)):.6f} "
    f"delivery_p50={delivery.get('med', 0):.2f}ms "
    f"delivery_p95={delivery.get('p(95)', 0):.2f}ms "
    f"delivery_p99={delivery.get('p(99)', 0):.2f}ms "
    f"connect_p99={connect.get('p(99)', 0):.2f}ms "
    f"gaps={gaps.get('count', 0):.0f} "
    f"duplicates={duplicates.get('count', 0):.0f}"
)

server = json.load(open(sys.argv[2], encoding="utf-8"))
measurements = {
    row.get("statistic"): row.get("value", 0)
    for row in server.get("measurements", [])
}
print(
    "server "
    f"publish_count={measurements.get('COUNT', 0):.0f} "
    f"publish_total={measurements.get('TOTAL_TIME', 0):.6f}s "
    f"publish_max={measurements.get('MAX', 0) * 1000:.2f}ms"
)
PY

if [[ "${KEEP_PERF_DATA:-false}" != "true" ]]; then
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v prefix="ws-perf-${RUN_ID}-%" -v ON_ERROR_STOP=1 -q <<'SQL'
CREATE TEMP TABLE perf_match_ids AS
  SELECT id FROM matches WHERE external_id LIKE :'prefix';
DELETE FROM match_outbox
  WHERE event_json->>'matchId' IN (SELECT id::text FROM perf_match_ids);
DELETE FROM matches WHERE id IN (SELECT id FROM perf_match_ids);
SQL
  echo "cleaned benchmark rows"
fi

echo "summary=$SUMMARY"
echo "service_metric=$SERVER_METRIC"
echo "service_log=$SERVICE_LOG"
if [[ "$INTEGRITY_STATUS" -ne 0 ]]; then
  exit "$INTEGRITY_STATUS"
fi
exit "$K6_STATUS"
