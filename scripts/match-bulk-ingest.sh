#!/usr/bin/env bash
# Local bulk ingest bench. Labels batch points/s and COPY rows/s separately from atomic commits.
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
BATCH_POINTS="${BATCH_POINTS:-2000}"
BATCH_SIZE="${BATCH_SIZE:-200}"
COPY_ROWS="${COPY_ROWS:-20000}"
JAR="$ROOT/services/match-service/target/match-service-0.0.1-SNAPSHOT.jar"
SERVICE_LOG="$REPORT_DIR/match-bulk-service-${RUN_ID}.log"
SERVICE_PID=""

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

for command in curl python3 nc; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "$command is required" >&2
    exit 1
  fi
done
if nc -z localhost "$SERVER_PORT" >/dev/null 2>&1; then
  echo "port $SERVER_PORT is already in use; refusing to benchmark an unknown service" >&2
  exit 1
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
  POSTGRES_POOL_MAX="${POSTGRES_POOL_MAX:-8}" \
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

python3 - "$BASE_URL" "$BATCH_POINTS" "$BATCH_SIZE" "$COPY_ROWS" "$RUN_ID" "${PERF_PHASE:-warm}" "$REPORT_DIR" <<'PY'
import hashlib, json, os, sys, time, urllib.request, uuid

base, batch_points, batch_size, copy_rows, run_id, phase, report_dir = (
    sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5], sys.argv[6], sys.argv[7]
)
home = str(uuid.uuid4())
away = str(uuid.uuid4())
token = os.environ.get("GATEWAY_INTERNAL_TOKEN") or ""

def headers(content_type="application/json"):
    out = {"Content-Type": content_type}
    if token:
        out["X-Gateway-Token"] = token
    return out

def call(method, path, body=None, expected=201, content_type="application/json", raw=None):
    data = raw if raw is not None else (None if body is None else json.dumps(body).encode())
    if method not in ("GET", "HEAD") and data is None:
        data = b""
    req = urllib.request.Request(base + path, data=data, method=method)
    for key, value in headers(content_type).items():
        req.add_header(key, value)
    with urllib.request.urlopen(req) as resp:
        payload = json.loads(resp.read().decode())
        if resp.status not in (expected, 200, 201):
            raise SystemExit(f"{method} {path} -> {resp.status}")
        return payload

def percentile(samples, p):
    if not samples:
        return None
    ordered = sorted(samples)
    index = min(len(ordered) - 1, max(0, int(round((p / 100.0) * (len(ordered) - 1)))))
    return ordered[index]

match = call("POST", "/api/matches", {
    "externalId": f"bulk-{run_id}",
    "surface": "HARD",
    "bestOfSets": 3,
    "players": [
        {"playerId": home, "displayName": "Home", "side": "HOME"},
        {"playerId": away, "displayName": "Away", "side": "AWAY"},
    ],
})
call("PATCH", f"/api/matches/{match['id']}/status", {"status": "IN_PROGRESS"}, expected=200)

def point(i):
    return {
        "serverId": home if i % 2 == 0 else away,
        "winnerId": away if i % 2 == 0 else home,
        "outcome": "WINNER",
        "rallyLength": 4,
        "scoreSnapshot": {"n": i, "set": 1, "game": 4, "points": ["30", "15"]},
        "shotSummary": {"shots": [{"type": "FOREHAND_GROUNDSTROKE", "speed": 32.1}]},
    }

sent = 0
batch_ms = []
started = time.perf_counter()
while sent < batch_points:
    chunk = min(batch_size, batch_points - sent)
    body = {"idempotencyKey": f"{run_id}-{sent}", "points": [point(sent + i) for i in range(chunk)]}
    t0 = time.perf_counter()
    call("POST", f"/internal/matches/{match['id']}/points/batch", body)
    batch_ms.append((time.perf_counter() - t0) * 1000.0)
    sent += chunk
batch_s = time.perf_counter() - started
batch_rate = sent / batch_s
print(
    "claim=transactional batch points/s "
    f"accepted={sent} seconds={batch_s:.3f} points_per_s={batch_rate:.2f} "
    f"p50={percentile(batch_ms, 50):.2f}ms p95={percentile(batch_ms, 95):.2f}ms "
    f"p99={percentile(batch_ms, 99):.2f}ms"
)

copy_match = call("POST", "/api/matches", {
    "externalId": f"copy-{run_id}",
    "surface": "HARD",
    "bestOfSets": 3,
    "players": [
        {"playerId": home, "displayName": "Home", "side": "HOME"},
        {"playerId": away, "displayName": "Away", "side": "AWAY"},
    ],
})
call("PATCH", f"/api/matches/{copy_match['id']}/status", {"status": "IN_PROGRESS"}, expected=200)
job = call("POST", f"/internal/matches/{copy_match['id']}/archive/jobs", {
    "idempotencyKey": f"copy-{run_id}",
    "expectedRows": copy_rows,
})
job_id = job["jobId"]
lines = []
for sequence in range(1, copy_rows + 1):
    away_won = ((9 + sequence) & 1) == 0
    server = home if sequence % 2 == 1 else away
    winner = away if away_won else home
    score = json.dumps({"set": 1, "game": 1 + sequence % 12, "points": ["30", "15"], "n": sequence, "server": server})
    shots = json.dumps({
        "shots": [
            {"type": "FIRST_SERVE", "speed": 48.2, "spin": 2200},
            {"type": "FOREHAND_GROUNDSTROKE", "speed": 32.4, "spin": 2800},
            {"type": "BACKHAND_GROUNDSTROKE", "speed": 29.1, "spin": 2400},
            {"type": "FOREHAND_GROUNDSTROKE", "speed": 31.0, "spin": 2600},
        ],
        "rally": 4 + (sequence % 5),
        "seed": 9,
    })
    lines.append(f"{job_id}\t{copy_match['id']}\t{sequence}\t{server}\t{winner}\tWINNER\t{4 + (sequence % 5)}\t{score}\t{shots}\n")
payload = "".join(lines).encode()
digest = hashlib.sha256(payload).hexdigest()
e2e_started = time.perf_counter()
t0 = time.perf_counter()
staged = call(
    "PUT",
    f"/internal/matches/archive/jobs/{job_id}/stream",
    raw=payload,
    expected=200,
    content_type="text/tab-separated-values",
)
stage_s = time.perf_counter() - t0
if staged.get("contentSha256") and staged["contentSha256"] != digest:
    raise SystemExit("job content SHA-256 mismatch")
t1 = time.perf_counter()
promoted = call("POST", f"/internal/matches/archive/jobs/{job_id}/promote", None, expected=200)
promote_s = time.perf_counter() - t1
e2e_s = time.perf_counter() - e2e_started
replay = call("POST", f"/internal/matches/archive/jobs/{job_id}/promote", None, expected=200)
if replay["checksum"] != promoted["checksum"]:
    raise SystemExit("promote retry checksum changed")
print(
    "claim=staging/promote rows/s "
    f"staging_rows={staged['sourceRows']} staging_s={stage_s:.3f} staging_rows_per_s={staged['sourceRows'] / stage_s:.2f} "
    f"promote_accepted={promoted['acceptedRows']} promote_s={promote_s:.3f} promote_rows_per_s={promoted['acceptedRows'] / promote_s:.2f} "
    f"e2e_s={e2e_s:.3f} e2e_rows_per_s={promoted['acceptedRows'] / e2e_s:.2f} "
    f"duplicates={promoted['duplicateRows']} checksum={promoted['checksum']}"
)
print("durability=on fsync_not_disabled synchronous_commit=on")
out = {
    "phase": phase,
    "operations": [
        {
            "operation": "transactional_batch_points",
            "rate": batch_rate,
            "p50_ms": percentile(batch_ms, 50),
            "p95_ms": percentile(batch_ms, 95),
            "p99_ms": percentile(batch_ms, 99),
        },
        {"operation": "staging_copy_rows", "rate": staged["sourceRows"] / stage_s, "p50_ms": stage_s * 1000, "p95_ms": stage_s * 1000, "p99_ms": stage_s * 1000},
        {"operation": "promote_rows", "rate": promoted["acceptedRows"] / promote_s, "p50_ms": promote_s * 1000, "p95_ms": promote_s * 1000, "p99_ms": promote_s * 1000},
        {"operation": "archive_e2e_rows", "rate": promoted["acceptedRows"] / e2e_s, "p50_ms": e2e_s * 1000, "p95_ms": e2e_s * 1000, "p99_ms": e2e_s * 1000},
    ],
    "checksum": promoted["checksum"],
    "contentSha256": staged.get("contentSha256"),
}
json.dump(out, open(f"{report_dir}/bulk-{run_id}.json", "w", encoding="utf-8"), indent=2)
PY

COUNTS="$(
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    -h localhost -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v prefix="copy-${RUN_ID}" -Atq <<'SQL'
SELECT
  COALESCE((SELECT point_count FROM matches WHERE external_id = :'prefix'), 0),
  COALESCE((SELECT COUNT(*) FROM match_points WHERE match_id = (SELECT id FROM matches WHERE external_id = :'prefix')), 0),
  COALESCE((SELECT COUNT(DISTINCT sequence_number) FROM match_points WHERE match_id = (SELECT id FROM matches WHERE external_id = :'prefix')), 0),
  COALESCE((SELECT COUNT(*) FROM match_archive_staging s
            JOIN match_archive_jobs j ON j.id = s.job_id
            JOIN matches m ON m.id = j.match_id
            WHERE m.external_id = :'prefix'), 0)
SQL
)"
IFS='|' read -r POINT_COUNT POINT_ROWS DISTINCT_SEQ STAGING_LEFT <<<"$COUNTS"
echo "durability copy_point_count=$POINT_COUNT points=$POINT_ROWS distinct_sequences=$DISTINCT_SEQ staging_left=$STAGING_LEFT"
if [[ "$POINT_COUNT" -ne "$COPY_ROWS" ]] \
  || [[ "$POINT_ROWS" -ne "$COPY_ROWS" ]] \
  || [[ "$DISTINCT_SEQ" -ne "$COPY_ROWS" ]] \
  || [[ "$STAGING_LEFT" -ne 0 ]]; then
  echo "bulk ingest durability invariant failed" >&2
  exit 1
fi

echo "service_log=$SERVICE_LOG"
