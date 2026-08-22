# Performance

Measured wins on the hottest paths. Sub-1 ms is an **in-process JMH p99** target, not a public HTTP claim. SLOs: [docs/slo.md](slo.md).

## What already shipped (Week 31)

| Area | Change | Why |
|---|---|---|
| Match list | `@EntityGraph(players)` + `point_count` column | Was 1+2N queries; list no longer counts points |
| Match get | Redis `live-match:{id}` snapshot, else DB row | Avoid loading point tapes for a count |
| Match indexes | `V3__match_list_indexes.sql` | Filtered catalogue sorts |
| Gateway API keys | Redis cache of successful validates (TTL 30s) | `/api/v1/**` was a user-service round-trip |
| Web rankings BFF | `Cache-Control: private, max-age=30, stale-while-revalidate=60` | Backend already Redis-cached |
| Match centre | `next/dynamic` court panel + `optimizePackageImports` | Keep 3D/chart code off the critical path |

## Program (after Phase 8)

Public path is gateway + tennis-data + match on Render **Starter**. Auth/user stay free and are out of production SLO gates.

| Layer | How |
|---|---|
| Catalogue k6 | `./scripts/k6-load.sh` — public GETs, 200-only, warm-up stage |
| `/api/v1` k6 | `tests/load/public-api-v1.js` — needs API key + warm user-service |
| JMH | `make jmh` — shaded uber-jar, 2 JVM forks, p99 < 1 ms |
| Correlation | `traceparent` / `X-Request-Id` from BFF through gateway |
| Live ticker | Client fetch of `/api/matches/ticker` (SWR 5s / 15s), not a full match list on every page |
| WebSocket | `NEXT_PUBLIC_MATCH_WS_URL` STOMP `/topic/matches/{id}` for deltas |
| Reports | `.run/performance/` (gitignored) |

```bash
export BASE_URL=https://api-gateway-lryh.onrender.com
SCENARIO=smoke ./scripts/k6-load.sh
SCENARIO=load ./scripts/k6-load.sh
# burst / soak as needed. Failure-mode checklist: ./scripts/perf-failure-modes.sh
```

Scenarios: `smoke` (30s, 1 VU), `load` (ramp to 8), `burst` (20 VUs), `soak` (30m).

JFR is local-only: `JFR_PID=... make jfr`. Never expose JFR or Prometheus on public Render.

## Regions (record, do not hide)

| Piece | Typical region | Note |
|---|---|---|
| Vercel | `lhr1` (London) | `apps/web/vercel.json` |
| Render | Dashboard (often US) | Blueprint does not pin region |
| Neon | Check console (`eu-west-2` in examples) | Cross-region RTT is reported, not migrated here |

## Measured (2026-08-22)

HTTP row: [tests/load/baselines/2026-08-22-smoke-live.md](../tests/load/baselines/2026-08-22-smoke-live.md).
Forked JMH: [tests/load/baselines/2026-08-22-jmh-forked.md](../tests/load/baselines/2026-08-22-jmh-forked.md).
CPU throughput: [tests/load/baselines/2026-08-22-cpu-decision-throughput.md](../tests/load/baselines/2026-08-22-cpu-decision-throughput.md).
Durable match writes: [tests/load/baselines/2026-08-22-durable-match-write.md](../tests/load/baselines/2026-08-22-durable-match-write.md).
Live WebSocket delivery: [tests/load/baselines/2026-08-22-live-websocket.md](../tests/load/baselines/2026-08-22-live-websocket.md).
Staged live capacity: [tests/load/baselines/2026-08-22-live-capacity.md](../tests/load/baselines/2026-08-22-live-capacity.md).
100k realistic staging: [tests/load/baselines/2026-08-22-live-100k-realistic.md](../tests/load/baselines/2026-08-22-live-100k-realistic.md).

| Layer | What we ran | Result vs SLO |
|---|---|---|
| JMH hot paths (local, 2 forks, Apple M1 Pro, Temurin 21.0.10) | `apiKeyHash`, state machine, rate-limit decision, tape aggregator | p99 61.3 µs / 208 ns / 167 ns / 333 ns (**hit** 1 ms) |
| JMH CPU decision bundle (local, 3 forks, 1 thread) | validation + rate-limit decision + eight-point aggregation | 12.40M ops/s; allocation 544 → 112 B/op |
| Durable match write (local, 8 VUs) | HTTP → point + counter + audit + transactional outbox in Postgres | 270.63 commits/s; p95 72.09 ms; p99 150.51 ms; 0 errors |
| Live WebSocket delivery (local, 100 clients) | Post-commit envelope → STOMP client; realistic and hot-topic profiles | p99 33 ms / 28 ms; 0 gaps, duplicates or errors |
| Live capacity stages (local, 20–40 clients) | Hot path, slow-client isolation, reconnect replay, two-node kill | healthy p99 15 / 37 / 13 / 58 ms; unrecovered gaps 0; not 100k |
| Live 100k realistic staging (local laptop) | Ramped 100 then 1,000 subscribers, 8 topics | 100: p99 30 ms pass; 1,000: p99 1.66 s fail; 100k not run |
| Warm public gateway (UK → live Render) | k6 smoke, 39× HTTP 200, 0 errors | p50 682 ms, p95 1.84 s, p99 1.97 s (**miss** 250/500 ms) |

Live lists are still unbounded (1450 players / 422 rankings / 230 in-progress matches in one GET). That payload plus UK→Render RTT is most of the HTTP time, not the Java hot paths.

## 100k live-delivery program

The target is 100,000 concurrent WebSocket clients across multiple service and load-generator instances. It is not a current capacity claim.

The ordered protocol foundation is now:

- Postgres allocates a monotonic sequence for each match in the same transaction as its event log and outbox row.
- WebSocket envelopes carry `eventId`, `sequence`, `occurredAt`, `commitObservedAt`, and the current snapshot.
- `GET /api/matches/{matchId}/events?afterSequence=N&limit=1000` supplies durable missed-event replay.
- `(match_id, sequence_number)` is unique, so duplicate sequence allocation fails rather than silently corrupting a stream.
- Every match-service instance subscribes to the same Redis Pub/Sub channel and republishes each envelope to its local STOMP clients.
- A keyed serial scheduler preserves per-match publication order while allowing unrelated matches to fan out in parallel.
- `match.live_publish_after_commit` measures from Spring's successful commit callback through Redis and each node's WebSocket broker enqueue.
- `make load-websocket` measures STOMP connection and client delivery latency while gating sequence gaps, duplicates, malformed frames, write failures, and p99 delivery.

`commitObservedAt` is captured immediately after Spring reports a successful commit. It is an honest application-side lower bound, not the database server's WAL flush timestamp. End-to-end client tests must measure from this field to receipt and report clock synchronization.

The local harness supports multiple service JVMs (`MATCH_INSTANCE_COUNT`), realistic topic distribution (`WS_MODE=realistic`), one hot topic (`WS_MODE=hot`), point bursts (`POINT_INTERVAL_MS=0`), controlled reconnects (`SUBSCRIBER_ITERATIONS` and `WS_HOLD_MS`), and slow consumers (`SLOW_CLIENT_PERCENT` and `SLOW_CLIENT_DELAY_MS`). It refuses more than 10,000 local clients by default because a single load generator cannot substantiate a 100k claim.

- Redis Pub/Sub is intentionally the low-latency broadcast plane, not the system of record. It is at-most-once. Postgres event logs and client sequence cursors remain the recovery plane; reconnect replay and process-failure proof are exercised by `make load-websocket-recovery` and the failover stage of `make load-live-capacity`.
- Slow clients are disconnected when they exceed the WebSocket send-time or send-buffer limit. Healthy clients keep receiving on a dedicated outbound pool. `make load-websocket-backpressure` gates p99 on `{client:normal}` only.
- Provider-neutral 100k jobs live in `infrastructure/kubernetes/live-100k/`. Apply them only after [tests/load/baselines/2026-08-22-live-100k-preflight.md](../tests/load/baselines/2026-08-22-live-100k-preflight.md). `make load-live-scale` ramps 100 → 100k and stops at the first failed gate.

No 100k result will be published until a distributed test records connected clients, successful subscriptions, **global** p50/p95/p99 delivery from Prometheus native histograms, disconnects, replay gaps, duplicate applications, server CPU/memory/GC, network throughput, load-generator saturation, and an immutable image digest. Local laptops cannot host this job. Per-worker k6 p99 values must not be averaged.

## Evidence template

Commit a row in `tests/load/baselines/` after a reviewed warm run:

```
date:
origin:
scenario:
hardware:
warmup:
p50_ms / p95_ms / p99_ms:
rps:
error_rate:
dataset:
limitations:
```

Distinguish **JMH**, **service**, **public API**, and **browser** numbers. Cold start is a separate dataset.

## Security note (API-key cache)

Revoke can lag up to **TTL seconds** (default 30). Keep TTL short.

## Explicitly deferred

- Cloudflare / edge CDN
- Paid Redis/Neon, region migration, service consolidation
- Analytics/replay production SLOs (no live ES/R2 yet)
