set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="infrastructure/docker/.env"
PORTS_FILE=".run/ports.env"
PID_DIR=".run/pids"
LOG_DIR=".run/logs"
COMPOSE_FILE="infrastructure/docker/docker-compose.yml"

mkdir -p "$PID_DIR" "$LOG_DIR"

set -a
# shellcheck disable=SC1090
[[ -f "$ENV_FILE" ]] && source "$ENV_FILE"
# shellcheck disable=SC1090
[[ -f "$PORTS_FILE" ]] && source "$PORTS_FILE"
set +a

: "${JAVA_HOME:=$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
if [[ -z "$JAVA_HOME" ]]; then
  echo "no JDK 21 found — install temurin-21" >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

compose() {
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --env-file "$PORTS_FILE" "$@"
}

start_service() {
  local name="$1" cmd="$2"
  local pid_file="$PID_DIR/$name.pid"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "  $name already running (pid $(cat "$pid_file"))"
    return 0
  fi
  echo "  starting $name → $LOG_DIR/$name.log"
  nohup bash -c "$cmd" >"$LOG_DIR/$name.log" 2>&1 &
  echo $! >"$pid_file"
}

wait_http() {
  local port="$1" label="$2" tries="${3:-120}"
  echo "  waiting for $label on :$port"
  for _ in $(seq 1 "$tries"); do
    if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1 \
      || curl -sf "http://localhost:$port/" >/dev/null 2>&1; then
      echo "  $label is up"
      return 0
    fi
    # Surface a crashed JVM immediately instead of burning the whole timeout.
    if [[ -f "$PID_DIR/$label.pid" ]] && ! kill -0 "$(cat "$PID_DIR/$label.pid")" 2>/dev/null; then
      echo "  $label exited early — last lines of $LOG_DIR/$label.log:" >&2
      tail -n 25 "$LOG_DIR/$label.log" >&2 || true
      return 1
    fi
    sleep 2
  done
  echo "  $label did not become healthy in time — see $LOG_DIR/$label.log" >&2
  tail -n 25 "$LOG_DIR/$label.log" >&2 || true
  return 1
}

echo "infrastructure"
compose --profile infra up -d postgres redis kafka minio elasticsearch
compose up -d --wait postgres redis kafka elasticsearch >/dev/null 2>&1 || true
./scripts/ensure-databases.sh

echo "services"
start_service eureka "SERVER_PORT=$EUREKA_SERVER_PORT ./mvnw -q -pl services/eureka-server spring-boot:run"
wait_http "$EUREKA_SERVER_PORT" eureka

start_service tennis-data "SERVER_PORT=$TENNIS_DATA_SERVER_PORT ./mvnw -q -pl services/tennis-data-service spring-boot:run"
wait_http "$TENNIS_DATA_SERVER_PORT" tennis-data

start_service match "SERVER_PORT=$MATCH_SERVER_PORT ./mvnw -q -pl services/match-service spring-boot:run"
start_service replay "SERVER_PORT=$REPLAY_SERVER_PORT ./mvnw -q -pl services/replay-service spring-boot:run"
start_service analytics "SERVER_PORT=$ANALYTICS_SERVER_PORT ./mvnw -q -pl services/analytics-service spring-boot:run"
start_service notification "SERVER_PORT=$NOTIFICATION_SERVER_PORT ./mvnw -q -pl services/notification-service spring-boot:run"
start_service web "pnpm --filter @tennisly/web dev"

cat <<EOF

stack starting — match/replay/analytics/notification/web still booting
  web            http://localhost:$WEB_PORT
  eureka         http://localhost:$EUREKA_SERVER_PORT
  tennis-data    http://localhost:$TENNIS_DATA_SERVER_PORT
  match          http://localhost:$MATCH_SERVER_PORT
  replay         http://localhost:$REPLAY_SERVER_PORT
  analytics      http://localhost:$ANALYTICS_SERVER_PORT
  notification   http://localhost:$NOTIFICATION_SERVER_PORT
  elasticsearch  http://localhost:$ELASTICSEARCH_PORT

  make status   # who's up
  make logs     # follow all app logs
  make health   # probe health endpoints
  make down     # stop everything
EOF
