# 2026-08-22 live WebSocket delivery

Local Postgres commit callback to STOMP client receipt. This is not a 100k-client or production claim.

## Environment

- Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- Temurin 21.0.10
- match-service and k6 on the host
- Postgres 16.11 and Redis 7 on local container ports
- Kafka and upstream ingestion disabled
- five unmeasured point writes per match before subscribers connect
- full `MatchLiveEventResponse` snapshots over raw WebSocket STOMP

## Realistic distribution

Configuration:

- 100 concurrent subscribers
- eight match topics
- one sequential writer per match
- ten-second write stage
- 250 ms writer pause

Results:

- client messages: 3,500
- connected: 100/100
- delivery p50: 6 ms
- delivery p95: 21 ms
- delivery p99: 33 ms
- delivery max: 35 ms
- STOMP connect p99: 160.01 ms
- write failures: 0
- sequence gaps: 0
- duplicate sequences: 0
- malformed frames: 0
- durability: 336 event-log rows = 336 outbox rows
- database sequence violations: 0

## Hot match

Configuration:

- 100 concurrent subscribers on one topic
- one sequential writer
- ten-second write stage
- 100 ms writer pause

Results:

- client messages: 7,400
- connected: 100/100
- delivery p50: 6 ms
- delivery p95: 19 ms
- delivery p99: 28 ms
- delivery max: 39 ms
- STOMP connect p99: 125.01 ms
- write failures: 0
- sequence gaps: 0
- duplicate sequences: 0
- malformed frames: 0
- durability: 81 event-log rows = 81 outbox rows
- database sequence violations: 0

## Two-node Redis fanout

Two independent match-service JVMs shared Postgres and Redis. HTTP writes entered node one, while subscribers were split evenly between both WebSocket nodes. Redis Pub/Sub broadcast each envelope to both local STOMP brokers.

The 100-client hot-match run passed:

- client messages: 7,200
- node one messages: 3,672
- node two messages: 3,528
- delivery p50: 8 ms
- delivery p95: 26 ms
- delivery p99: 35 ms
- delivery max: 56 ms
- write failures, sequence gaps, duplicates and malformed frames: 0
- durability: 79 event-log rows = 79 outbox rows
- database sequence violations: 0

The two-node realistic run used 100 clients, eight topics and eight concurrent writers. It delivered 3,100 messages across both nodes with zero correctness failures, but failed the latency gate at 109 ms p99:

- delivery p50: 12 ms
- delivery p95: 78 ms
- delivery p99: 109 ms
- delivery max: 123 ms
- durability: 304 event-log rows = 304 outbox rows

This is a capacity limit of the all-on-one-laptop topology, not a passing realistic-distribution result. The service JVMs, Postgres, Redis and k6 competed for the same ten CPU cores. Distributed capacity evidence remains required.

## Reconnect exercise

Twenty hot-topic clients completed three connections each. The harness observed 40 planned reconnects, 60/60 successful STOMP connections, 580 messages, 9 ms delivery p99, and zero gaps or duplicates.

This run validates reconnect and resubscribe measurement. Missed-event replay, slow-client isolation and node-kill recovery are in [2026-08-22-live-capacity.md](2026-08-22-live-capacity.md).

## Cold-run observation

The first un-warmed ten-client run measured 57 ms delivery p99 and correctly failed the 50 ms gate. After adding explicit unmeasured point warm-up, the equivalent ten-client run measured 9 ms p99 with zero correctness failures.

## Reproduction

```bash
make load-websocket
WS_MODE=hot WS_CLIENTS=100 POINT_INTERVAL_MS=100 make load-websocket
MATCH_INSTANCE_COUNT=2 WS_MODE=hot WS_CLIENTS=100 POINT_INTERVAL_MS=100 make load-websocket
SUBSCRIBER_ITERATIONS=3 WS_HOLD_MS=3000 make load-websocket
```

The harness stores raw output under `.run/performance/`, validates durable event/outbox counts, verifies per-match sequence continuity, removes only rows bearing its run prefix, and refuses more than 10,000 local clients unless explicitly overridden.

## Limitations

- Load generator, service and clocks share one laptop.
- `commitObservedAt` is captured by Spring immediately after commit, not by the Postgres WAL.
- Each node still uses Spring's in-memory simple broker for its own connected clients.
- Redis Pub/Sub is at-most-once; Postgres sequence replay, not Redis, is the recovery source.
- Distributed 100k load remains unproven. Local backpressure, replay and node-kill evidence is in [2026-08-22-live-capacity.md](2026-08-22-live-capacity.md).
