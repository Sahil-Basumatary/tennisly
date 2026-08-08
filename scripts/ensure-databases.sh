#!/usr/bin/env bash
set -euo pipefail

# Compose init scripts only run when the postgres volume is first created, so a
# machine that predates a new service keeps booting without its database. This
# reconciles the running instance with the databases the services expect.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="infrastructure/docker/.env"
PORTS_FILE=".run/ports.env"
COMPOSE_FILE="infrastructure/docker/docker-compose.yml"

set -a
# shellcheck disable=SC1090
[[ -f "$ENV_FILE" ]] && source "$ENV_FILE"
# shellcheck disable=SC1090
[[ -f "$PORTS_FILE" ]] && source "$PORTS_FILE"
set +a

DB_USER="${POSTGRES_USER:-tennisly}"
DATABASES=(
  tennisly_users
  tennisly_auth
  tennisly_matches
  tennisly_tennis_data
  tennisly_replay
  tennisly_analytics
  tennisly_notifications
  tennisly_billing
)

compose() {
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --env-file "$PORTS_FILE" "$@"
}

psql_postgres() {
  compose exec -T postgres psql -U "$DB_USER" -d postgres "$@"
}

for db in "${DATABASES[@]}"; do
  exists="$(psql_postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$db';" 2>/dev/null || true)"
  if [[ "$exists" == "1" ]]; then
    continue
  fi
  echo "  creating missing database $db"
  psql_postgres -c "CREATE DATABASE $db;" >/dev/null
  compose exec -T postgres psql -U "$DB_USER" -d "$db" \
    -c 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS "pgcrypto";' >/dev/null
done
