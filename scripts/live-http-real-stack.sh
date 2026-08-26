#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PERF_REPORT_DIR:-$ROOT/.run/performance}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
SERVER_PORT="${HTTP_LIVE_MATCH_PORT:-18096}"
EDGE_PORT="${HTTP_LIVE_EDGE_PORT:-18097}"
WRITE_URL="http://127.0.0.1:${SERVER_PORT}"
BASE_URL="${BASE_URL:-http://127.0.0.1:${EDGE_PORT}}"
CLIENTS="${HTTP_LIVE_CLIENTS:-100}"
HOLD_S="${HTTP_LIVE_HOLD_S:-30}"
RAMP_S="${HTTP_LIVE_RAMP_S:-3}"
POINT_INTERVAL_MS="${POINT_INTERVAL_MS:-15000}"
POSTGRES_PORT="${POSTGRES_PORT:-15432}"
POSTGRES_USER="${POSTGRES_USER:-tennisly}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-tennisly_dev}"
POSTGRES_DB="${POSTGRES_DB_MATCHES:-tennisly_matches}"
REDIS_PORT="${REDIS_PORT:-16379}"
JAR="$ROOT/services/match-service/target/match-service-0.0.1-SNAPSHOT.jar"
SUMMARY="$REPORT_DIR/k6-live-http-real-${CLIENTS}-${RUN_ID}.json"
OUTPUT="$REPORT_DIR/k6-live-http-real-${CLIENTS}-${RUN_ID}.txt"
SERVICE_LOG="$REPORT_DIR/live-http-real-service-${RUN_ID}.log"
EDGE_LOG="$REPORT_DIR/live-http-real-edge-${RUN_ID}.log"

mkdir -p "$REPORT_DIR"

if [[ ! "$CLIENTS" =~ ^[0-9]+$ ]] || [[ "$CLIENTS" -lt 1 ]]; then
  echo "HTTP_LIVE_CLIENTS must be a positive integer" >&2
  exit 1
fi
if [[ "$CLIENTS" -gt 2000 ]]; then
  echo "Refusing more than 2000 local HTTP live clients on one laptop" >&2
  exit 1
fi

HOST="$(python3 - <<'PY' "$BASE_URL"
import sys
from urllib.parse import urlparse
print(urlparse(sys.argv[1]).hostname or "")
PY
)"
if [[ "$HOST" != "127.0.0.1" && "$HOST" != "localhost" && "${ALLOW_VERCEL_LIVE_HTTP:-0}" != "1" ]]; then
  echo "Refusing remote BASE_URL without ALLOW_VERCEL_LIVE_HTTP=1 (Hobby quotas)" >&2
  exit 1
fi

for command in curl k6 nc python3; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "$command is required" >&2
    exit 1
  fi
done
if nc -z localhost "$SERVER_PORT" >/dev/null 2>&1; then
  echo "port $SERVER_PORT is already in use; refusing to test an unknown service" >&2
  exit 1
fi
if nc -z localhost "$EDGE_PORT" >/dev/null 2>&1; then
  echo "port $EDGE_PORT is already in use; refusing to test an unknown edge" >&2
  exit 1
fi
if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required" >&2
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

SERVICE_PID=""
EDGE_PID=""
stop_services() {
  if [[ -n "$EDGE_PID" ]]; then
    kill "$EDGE_PID" 2>/dev/null || true
  fi
  if [[ -n "$SERVICE_PID" ]]; then
    kill "$SERVICE_PID" 2>/dev/null || true
    wait "$SERVICE_PID" 2>/dev/null || true
  fi
}
trap stop_services EXIT

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
  TENNISLY_KAFKA_ENABLED=false \
  MATCH_INGEST_ENABLED=false \
  EUREKA_CLIENT_ENABLED=false \
  SPRING_CLOUD_CONFIG_ENABLED=false \
  MANAGEMENT_HEALTH_KAFKA_ENABLED=false \
  LOGGING_LEVEL_DEV_SAHILBASUMATARY_MATCHSERVICE=ERROR \
  java -jar "$JAR" >"$SERVICE_LOG" 2>&1 &
SERVICE_PID=$!

for _ in $(seq 1 90); do
  if curl -fsS "$WRITE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$SERVICE_PID" >/dev/null 2>&1; then
    echo "match-service exited; see $SERVICE_LOG" >&2
    exit 1
  fi
  sleep 1
done
if ! curl -fsS "$WRITE_URL/actuator/health" >/dev/null; then
  echo "match-service did not become healthy; see $SERVICE_LOG" >&2
  exit 1
fi

python3 "$ROOT/scripts/live-http-edge.py" \
  --upstream-port "$SERVER_PORT" \
  --edge-port "$EDGE_PORT" \
  --point-interval "$(awk "BEGIN { printf \"%.3f\", ${POINT_INTERVAL_MS}/1000 }")" \
  >"$EDGE_LOG" 2>&1 &
EDGE_PID=$!
for _ in $(seq 1 25); do
  if curl -sf "http://127.0.0.1:${EDGE_PORT}/metrics" >/dev/null; then
    break
  fi
  sleep 0.2
done

echo "write=$WRITE_URL edge=$BASE_URL clients=$CLIENTS interval_ms=$POINT_INTERVAL_MS"
k6_status=0
k6 run \
  --summary-export "$SUMMARY" \
  -e BASE_URL="$BASE_URL" \
  -e MATCH_WRITE_URL="$WRITE_URL" \
  -e RUN_ID="$RUN_ID" \
  -e HTTP_LIVE_CLIENTS="$CLIENTS" \
  -e HTTP_LIVE_HOLD_S="$HOLD_S" \
  -e HTTP_LIVE_RAMP_S="$RAMP_S" \
  -e POINT_INTERVAL_MS="$POINT_INTERVAL_MS" \
  "$ROOT/tests/load/live-http-real.js" | tee "$OUTPUT" || k6_status=$?

curl -sf "http://127.0.0.1:${EDGE_PORT}/metrics" | tee "$REPORT_DIR/live-http-real-origin-${CLIENTS}-${RUN_ID}.json" || true
echo "wrote $SUMMARY"
exit "$k6_status"
