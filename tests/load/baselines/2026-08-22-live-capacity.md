# 2026-08-22 live capacity stages

Local laptop evidence for backpressure, reconnect replay, and node-kill recovery. This is not a 100k-client or production claim.

## Environment

- Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- Temurin 21.0.10
- match-service and k6 on the host
- Postgres 16.11 and Redis 7 on local container ports
- Kafka and upstream ingestion disabled
- `make load-live-capacity` stages, then re-measured failing stages after harness fixes
- latency is `now - commitObservedAt` on in-order live frames
- slow-client samples are tagged `{client:slow}` and excluded from the p99 gate

## What failed first, then what changed

The first combined run (`.run/performance/live-capacity-20260822T161248Z.txt`) passed sanity-hot and failed the other three stages:

- backpressure healthy p99 66 ms against a 50 ms gate (0 unrecovered gaps)
- recovery-replay 1 unrecovered gap (p99 43 ms, 438 recovered)
- failover p99 241 ms against an 80 ms gate (0 unrecovered gaps, 20 failovers)

Fixes before the numbers below:

- live sequence jumps are ignored until HTTP replay fills the hole (`ahead`), not counted as loss
- replay HTTP runs before reconnect, never inside the STOMP message handler
- send time/buffer limits 500 ms / 16 KiB; slow-client delay 250 ms

## Stages

| Stage | Clients | Topology | p50 | p95 | p99 | Gate | Unrecovered | Recovered | Result |
|---|---:|---|---:|---:|---:|---:|---:|---:|---|
| sanity-hot | 20 | 1 node, 1 topic | 6 ms | 12 ms | 15 ms | 50 ms | 0 | — | pass |
| backpressure | 40 (20% slow) | 1 node, 1 topic | 8 ms | 25 ms | 37 ms | 50 ms healthy | 0 | — | pass |
| recovery-replay | 20 × 3 sessions | 1 node, 1 topic | 5 ms | 9 ms | 13 ms | 50 ms | 0 | 460 | pass |
| failover | 20 × 3 sessions | 2 nodes, kill node 2 at 6s | 5 ms | 33 ms | 58 ms | 80 ms | 0 | 169 | pass |

p50 is k6 `med` of `live_ws_delivery_ms{client:normal}`.

### sanity-hot

- 1,200 client messages, 20/20 connected
- write failures, sequence gaps, duplicates, malformed frames: 0
- durability: event-log rows = outbox rows, 0 sequence violations

### backpressure

- 1,764 client messages; 630 tagged slow
- healthy-client p99 37 ms; combined p99 including slow clients is seconds by construction
- server disconnects slow sessions rather than blocking the fanout pool (`AbortPolicy`, 500 ms send, 16 KiB buffer)
- unrecovered gaps: 0

### recovery-replay

- 40 planned reconnects, 60/60 STOMP connections
- 640 live messages + 460 events filled from `GET /api/matches/{id}/events`
- unrecovered gaps: 0
- replay HTTP failures: 0

### failover

- node 2 killed at 6s; writers stayed on node 1
- 20 failovers onto the surviving WebSocket URL
- 1,304 live messages + 169 replayed events
- unrecovered gaps: 0
- curl to the dead node port after the run is expected

## Reproduction

```bash
make load-websocket-backpressure
make load-websocket-recovery
make load-live-capacity
```

Raw k6 JSON stays under `.run/performance/` (gitignored).

## 100k manifests

Provider-neutral objects live in `infrastructure/kubernetes/live-100k/`. They have not been executed. Do not read this file as a 100k result.

## Limitations

- Load generator, service JVMs, Postgres, Redis and clocks share one laptop.
- `commitObservedAt` is the Spring after-commit timestamp, not the Postgres WAL flush.
- Each node still uses Spring's in-memory simple broker for its own sockets; Redis Pub/Sub is the cross-node bus and is at-most-once.
- Failover p99 uses an 80 ms gate because reconnect and node-loss are a different SLO than the healthy 50 ms path.
- Two-node eight-topic 100-client realistic fanout previously missed 50 ms (109 ms p99); that remains a laptop topology limit, not re-run here.
