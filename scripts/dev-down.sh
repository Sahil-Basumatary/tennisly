#!/usr/bin/env bash
# Stop the local Tennisly app services and infrastructure containers.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="infrastructure/docker/.env"
PORTS_FILE=".run/ports.env"
PID_DIR=".run/pids"
COMPOSE_FILE="infrastructure/docker/docker-compose.yml"

# Maven forks the JVM, so killing the recorded shell alone leaves java running.
kill_tree() {
  local pid="$1" child
  for child in $(pgrep -P "$pid" 2>/dev/null); do
    kill_tree "$child"
  done
  kill "$pid" 2>/dev/null || true
}

for name in web replay match tennis-data eureka; do
  pid_file="$PID_DIR/$name.pid"
  [[ -f "$pid_file" ]] || continue
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "stopping $name (pid $pid)"
    kill_tree "$pid"
  fi
  rm -f "$pid_file"
done

compose_args=(-f "$COMPOSE_FILE" --env-file "$ENV_FILE")
[[ -f "$PORTS_FILE" ]] && compose_args+=(--env-file "$PORTS_FILE")
docker compose "${compose_args[@]}" --profile infra --profile tools down

echo "down"
