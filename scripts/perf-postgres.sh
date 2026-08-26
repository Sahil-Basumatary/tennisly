#!/usr/bin/env bash
# Ephemeral Postgres 16 for local evidence. Does not touch tennisly-postgres data.
set -euo pipefail

NAME="${PERF_POSTGRES_NAME:-tennisly-perf-postgres}"
PORT="${PERF_POSTGRES_PORT:-15442}"
IMAGE="${PERF_POSTGRES_IMAGE:-postgres:16-alpine}"
USER_NAME="${POSTGRES_USER:-tennisly}"
PASSWORD="${POSTGRES_PASSWORD:-tennisly_dev}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INIT="$ROOT/infrastructure/docker/init-scripts"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for ephemeral evidence Postgres" >&2
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  docker rm -f "$NAME" >/dev/null
fi

docker run -d --name "$NAME" \
  -e POSTGRES_USER="$USER_NAME" \
  -e POSTGRES_PASSWORD="$PASSWORD" \
  -e POSTGRES_DB=tennisly \
  -p "${PORT}:5432" \
  -v "$INIT:/docker-entrypoint-initdb.d:ro" \
  "$IMAGE" \
  postgres \
    -c fsync=on \
    -c synchronous_commit=on \
    -c wal_compression=on \
    -c shared_buffers=256MB >/dev/null

for _ in $(seq 1 40); do
  if docker exec "$NAME" pg_isready -U "$USER_NAME" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! docker exec "$NAME" pg_isready -U "$USER_NAME" >/dev/null 2>&1; then
  echo "ephemeral Postgres did not become ready" >&2
  exit 1
fi

for _ in $(seq 1 40); do
  if PGPASSWORD="$PASSWORD" psql -h localhost -p "$PORT" -U "$USER_NAME" -d tennisly_matches -Atqc "select 1" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! PGPASSWORD="$PASSWORD" psql -h localhost -p "$PORT" -U "$USER_NAME" -d tennisly_matches -Atqc "select 1" >/dev/null 2>&1; then
  echo "ephemeral Postgres did not create tennisly_matches" >&2
  docker logs "$NAME" >&2 || true
  exit 1
fi

SHOW="$(
  PGPASSWORD="$PASSWORD" psql -h localhost -p "$PORT" -U "$USER_NAME" -d tennisly_matches -Atq <<'SQL'
SELECT
  current_setting('fsync') || '|' ||
  current_setting('synchronous_commit') || '|' ||
  current_setting('wal_compression') || '|' ||
  current_setting('shared_buffers');
SQL
)"
IFS='|' read -r FSYNC SYNCHRONOUS WAL BUFFERS <<<"$SHOW"
echo "ephemeral_postgres name=$NAME port=$PORT fsync=$FSYNC synchronous_commit=$SYNCHRONOUS wal_compression=$WAL shared_buffers=$BUFFERS"
echo "POSTGRES_PORT=$PORT"
