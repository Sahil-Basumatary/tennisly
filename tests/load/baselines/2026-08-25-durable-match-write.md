# 2026-08-25 atomic four-row point commits

This is local HTTP → Postgres durable throughput. It is not an in-memory operation count, not bulk COPY, and not a production Render result.

## Commit definition

One successful `POST /api/matches/{id}/points` returns HTTP 201 only after one Postgres transaction commits:

- the new `match_points` row
- one `UPDATE matches ... RETURNING` of `point_count`, `live_sequence`, and `current_score`
- the `match_event_logs` audit row
- the `match_outbox` event row

Redis snapshot and Kafka realtime publication run after commit. Kafka was disabled. `fsync` was not disabled. `synchronous_commit=on`.

## Environment

- date: 2026-08-25T21:44Z
- hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- runtime: Temurin 21.0.10
- match-service and k6 on the host; Postgres 16.11 Alpine and Redis 7 in local Docker
- ports: Postgres 15432, Redis 16379, match-service 18094
- Hikari pool: 16
- eight measured virtual users, each writing sequentially to its own match
- ten-second two-VU warmup followed by a 30-second measured stage
- observed Postgres: `synchronous_commit=on`, `wal_compression=off`, `shared_buffers=128MB`
- compose now requests `shared_buffers=256MB` and `wal_compression=on` on next recreate; this run used the already-running container

## Previous published row (2026-08-22)

JPA entity-graph writes, outbox in the transaction, post-commit fanout:

- 8,119 measured commits
- 270.63 commits/s
- p50 20.99 ms / p95 72.09 ms / p99 150.51 ms
- errors 0

## This run (JDBC hot-path writer)

Slim snapshot read, one `UPDATE ... RETURNING`, prepared JDBC inserts, post-commit fanout only (`fanoutAfterCommit`, no second outbox write):

- measured commits: 19,160
- durable throughput: **638.67 commits/s**
- p50: 10.03 ms
- p95: 24.87 ms
- p99: 41.56 ms
- errors: 0
- durability verification: 21,581 point counters = point rows = audit events = outbox events; sequence violations 0

Observed change versus 2026-08-22:

- 2.36× durable throughput
- 52.2% lower p50
- 65.5% lower p95
- 72.4% lower p99
- zero missing or duplicate durable records

The run passes the local p95 < 100 ms, p99 < 250 ms, and error rate < 0.1% gates.

## Reproduction

`make load-durable`. The harness refuses non-loopback targets, starts its own match-service on port 18094, verifies all four Postgres durability counts, and stores raw output under `.run/performance/`.

This does not prove 639 concurrent users, multi-node behavior, Kafka acknowledgement, or crash recovery. It proves 638.67 committed point transactions/s for this local single-instance topology.
