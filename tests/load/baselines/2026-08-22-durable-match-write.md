# 2026-08-22 durable match-write benchmark

This is local durable HTTP throughput, not an in-memory operation count and not a production Render result.

## Commit definition

One successful `POST /api/matches/{id}/points` returns HTTP 201 only after one Postgres transaction commits:

- the new `match_points` row;
- the updated `matches.point_count` and score;
- the `match_event_logs` audit row;
- the `match_outbox` event row.

Redis snapshot and Kafka realtime publication run after commit. Kafka was disabled for this run. Blank external fanout clients make the scheduled outbox relay a no-op after reading the durable row.

## Environment

- Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- Temurin 21.0.10
- match-service and k6 on the host
- Postgres 16.11 Alpine and Redis 7 in local Docker
- Hikari pool: 10
- eight measured virtual users, each writing sequentially to its own match
- ten-second two-VU warmup followed by a 30-second measured stage
- 20 isolated benchmark matches covering every global k6 VU slot

## Before

Every request cached Redis and published Kafka inside the database transaction, then submitted an immediate outbox-drain task after commit. The bounded executor eventually pushed drain work back onto request threads.

- measured commits: 4,677
- durable throughput: 155.90 commits/s
- p50: 34.07 ms
- p95: 129.42 ms
- p99: 367.70 ms
- errors: 0
- durability verification: 5,232 point counters = point rows = audit events = outbox events

## After

The transaction writes the outbox first. Realtime Redis/Kafka fanout starts only after a successful commit, and scheduled polling owns external outbox delivery.

- measured commits: 8,119
- durable throughput: 270.63 commits/s
- p50: 20.99 ms
- p95: 72.09 ms
- p99: 150.51 ms
- errors: 0
- durability verification: 9,378 point counters = point rows = audit events = outbox events

Observed change:

- 73.6% higher durable throughput
- 38.4% lower p50
- 44.3% lower p95
- 59.1% lower p99
- zero missing or duplicate durable records

The run passes the local p95 < 100 ms, p99 < 250 ms and error rate < 0.1% gates.

## Reproduction

Run `make load-durable`. The harness refuses non-loopback targets, starts its own match-service on port 18094, verifies all four Postgres durability counts, removes only rows bearing its run prefix, and stores raw output under `.run/performance/`.

This does not prove 270 concurrent users, multi-node behavior, cross-region latency, Kafka acknowledgement, external fanout capacity or crash recovery. It proves 270.63 committed point transactions/s for this local single-instance topology.
