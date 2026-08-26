# Performance

Measured wins on the hottest paths. Sub-1 ms is an **in-process JMH p99** on a CPU pipeline (decode, validation, state transition, event serialization). It is not public HTTP, not Postgres commit time, and not replay generation. SLOs: [docs/slo.md](slo.md).

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
| JMH | `make jmh` — shaded uber-jar, 2 JVM forks, in-process CPU p99 < 1 ms |
| Correlation | `traceparent` / `X-Request-Id` from BFF through gateway |
| Live ticker | Anonymous CDN cache of `/api/matches/ticker` (`s-maxage=3`, ETag/304) |
| Live scores | Compact `/api/matches/{id}/live` and `/cursor` (`s-maxage=2`); completed matches immutable |
| Recovery | `GET /api/matches/{id}/events?afterSequence=` stays private `no-store` |
| WebSocket | Opt-in (`NEXT_PUBLIC_LIVE_TRANSPORT=hybrid`); HTTP is the public default |
| Reports | `.run/performance/` (gitignored) |

```bash
export BASE_URL=https://api-gateway-lryh.onrender.com
SCENARIO=smoke ./scripts/k6-load.sh
SCENARIO=load ./scripts/k6-load.sh
# burst / soak as needed. Failure-mode checklist: ./scripts/perf-failure-modes.sh
```

Scenarios: `smoke` (30s, 1 VU), `load` (ramp to 8), `burst` (20 VUs), `soak` (30m).

JFR is local-only: `JFR_PID=... make jfr`. Never expose JFR or Prometheus on public Render.

## Same-budget near-live HTTP

The public default is **near-live scores**, not 100k WebSockets. Animated court replay stays opt-in and cacheable. Writes are unchanged: HTTP 201 only after the atomic Postgres point + counter + event log + outbox commit.

| Audience request | Edge TTL | Origin work |
|---|---|---|
| Ticker strip | `s-maxage=3`, `stale-while-revalidate=10`, 10s client / 30s hidden tab | Redis `live-ticker:v1`, else two bounded status pages (size 12) |
| Match-centre score | `s-maxage=2` while live; 24h immutable when completed | Compact live document keyed by `liveSequence` |
| Court point tape | sealed sequences `immutable`; newest point `s-maxage=2` | Replay-service miss only; engine version is part of the cache key |
| Event recovery | never CDN-cached; BFF single-flight + Redis 1s private page | genuine gaps only (`liveSequence > cursor + 1`); join and one-step skip `/events` |

Vercel Hobby included usage (do not burn this in a 100k demo): **100 GB Fast Data Transfer**, **10 GB Fast Origin Transfer**, **1M function invocations**, **4 CPU-hrs**. Cache hits are CDN bandwidth, not Render origin RPS. A 100k-viewer *architecture* is compact HTTP + collapse at the edge. It is not a measured 100k concurrent viewer run.

Target freshness: **2–3 seconds** for match-centre scores, **~10 seconds** for the ticker. ETags exist so unchanged sequences cost a 304, not another JSON body.

Do not set `ALLOW_VERCEL_LIVE_HTTP=1` against tennisly.tv unless you have checked the Hobby dashboard. Local proof is `make load-http-live` (100, then 1000) and `make load-http-live-stack` against match-service + Postgres + Redis. The Python origin is a CDN stand-in. It is not Vercel.

WebSocket wake-up remains for staging: `NEXT_PUBLIC_LIVE_TRANSPORT=hybrid` and `NEXT_PUBLIC_MATCH_WS_URL`. Public production stays HTTP.

Stampede dataset (2s mock cadence, recover-on-join): [tests/load/baselines/2026-08-26-http-live-cache.md](../tests/load/baselines/2026-08-26-http-live-cache.md).
Recovery-fixed evidence (20s cadence, skip join/one-step): [tests/load/baselines/2026-08-26-http-live-recovery.md](../tests/load/baselines/2026-08-26-http-live-recovery.md).

## Regions (record, do not hide)

| Piece | Typical region | Note |
|---|---|---|
| Vercel | `lhr1` (London) | `apps/web/vercel.json` |
| Render | Dashboard (often US) | Blueprint does not pin region |
| Neon | Check console (`eu-west-2` in examples) | Cross-region RTT is reported, not migrated here |

## Measured (2026-08-25)

Replay physics: [tests/load/baselines/2026-08-25-replay-physics.md](../tests/load/baselines/2026-08-25-replay-physics.md).
Archive tape: [tests/load/baselines/2026-08-25-archive-tape.md](../tests/load/baselines/2026-08-25-archive-tape.md).
Atomic match writes: [tests/load/baselines/2026-08-25-durable-match-write.md](../tests/load/baselines/2026-08-25-durable-match-write.md).
Bulk ingest: [tests/load/baselines/2026-08-25-bulk-ingest.md](../tests/load/baselines/2026-08-25-bulk-ingest.md).

| Layer | What we ran | Result |
|---|---|---|
| Replay full pipeline (local JMH, 1 fork, Apple M1 Pro, Temurin 21.0.10) | solver + frames for one production-shaped point | **14,596 frames/s**; assembler-only 30.8M frames/s is a different claim |
| Archive tape (local JMH, 1 fork, 1024m heap) | 1,000,000 unique generated events, 8 workers | **36.2 million-event tapes/s**; not HTTP or Postgres |
| Durable match write (local, 8 VUs) | HTTP → point + counter + audit + outbox in Postgres | **638.67 commits/s**; p95 24.87 ms; p99 41.56 ms; 0 errors |
| Bulk ingest (local, labelled separately) | transactional batches then COPY/staging | **5,458 points/s** batch; **20,991 rows/s** COPY of 1,000,000 source rows |

`make jmh` still gates only in-process CPU p99 and point-decision throughput. Scale benches are `make jmh-replay` and `make jmh-archive`. Multi-fork evidence with JFR, cold/warm HTTP, and an ephemeral Postgres is `make perf-evidence`. Frame counts for derived replay frames/s come from [tests/load/baselines/replay-golden-sha256.txt](../tests/load/baselines/replay-golden-sha256.txt), not shell literals.

The 2026-08-25 table above is **historical single-run / single-fork** evidence. Do not treat those rows as a 3-fork distribution. Replay engine 2.0.0 replaces 1.0.0 generation; stored 1.0.0 artifacts remain readable.

## Measured (2026-08-26, engine 2.0 / streaming ingest)

Generated from session JSON: [tests/load/baselines/2026-08-26-v2-evidence.md](../tests/load/baselines/2026-08-26-v2-evidence.md). Local floors (20% slack): [tests/load/baselines/v2-floors.json](../tests/load/baselines/v2-floors.json). Absolute floors apply to this suite, not PR CI.

| Layer | What we ran | Result |
|---|---|---|
| In-process CPU pipeline (3 forks) | decode + validate + state machine + event serialize | p99 **13.2 µs** (not HTTP) |
| Replay full pipeline (3 forks, engine 2.0.0) | solver + frames for one production-shaped point | **27,962 frames/s**; 50k/100k/250k ladders **missed**; assembler-only 23.3M frames/s is a different claim |
| Archive tape (3 forks, in-memory) | 1,000,000 events | **8.9M / 27.0M events/s** at 1 / 8 workers; not Postgres |
| Atomic match write (ephemeral PG 16, fsync=on, 1 cold + 5 warm) | HTTP → point + counter + audit + outbox | median **251.9 commits/s**; cold p99 69.7 ms; some warm runs missed 250 ms p99 (recorded, not hidden) |
| Streaming bulk ingest (same Postgres) | transactional batches, then COPY staging + promote | median **2,744 batch points/s**; **25,608 staging rows/s**; **26,297 promote rows/s** |

HTTP stages were 10 s measured windows and 12,000 COPY rows so cold vs warm p99 is comparable. That is not a 30 s soak. Durability checks (counts, distinct sequences, staging cleaned) passed on every completed run.

## Measured (2026-08-26, near-live HTTP)

Stampede dataset (do not overwrite): [tests/load/baselines/2026-08-26-http-live-cache.md](../tests/load/baselines/2026-08-26-http-live-cache.md). Recovery-fixed + real stack: [tests/load/baselines/2026-08-26-http-live-recovery.md](../tests/load/baselines/2026-08-26-http-live-recovery.md). Replay with `make load-http-live` and `make load-http-live-stack`.

| Layer | What we ran | Result |
|---|---|---|
| HTTP live cache-collapse (Python origin + edge, 100 VUs, 2s cadence) | recover-on-join stampede | **65.6×** score collapse (787 / 12); **350 uncached `/events`**; 0 errors; p50 0.68 ms; p95 14.8 ms |
| HTTP live cache-collapse (1,000 VUs, 2s cadence) | same stampede harness | **565×** score collapse (7,919 / 14); **3,568 `/events` of 3,582 origin fetches**; p99 293 ms |
| HTTP live recovery-fixed (Python origin + edge, 100 VUs, 20s cadence) | skip join/one-step; 3s + 25s | **120×** (2,644 / 22 origin); **0 recovery**; 99.16% score hits; p50 0.88 ms; p95 5.82 ms; p99 11.5 ms |
| HTTP live recovery-fixed (1,000 VUs, 20s cadence) | same, 5s + 25s | **604×** (14,496 / 24 origin); **0 recovery**; 99.83% score hits; p50 0.50 ms; p95 38.6 ms; p99 55.9 ms |
| HTTP live real stack (match-service + Postgres + Redis + edge, 100 VUs) | 15s point writes | **142×** (3,554 / 25); **0 recovery**; 99.29% hits; viewer p99 14.6 ms |
| HTTP live real stack (1,000 VUs) | same | **597×** (16,722 / 28); **0 recovery**; 99.83% hits; viewer p99 64.4 ms |
| Vercel Hobby / 100k viewers | not run | Hobby quotas not burned; 100k is an architecture, not a measured ceiling |

The 1,000-VU p99 is still the local edge stand-in, not Vercel. 99.83% score-cache hits is the s-maxage=2 / 3s-poll ceiling at 1,000 VUs (about 0.5 origin revalidates per second vs ~440 viewer RPS). 99.9% is the 10k-viewer arithmetic at the same TTLs, not a 1k-laptop gate. Court replay was not in this load.

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

The target is 100,000 concurrent **viewers**. The public transport for that shape is compact, edge-cached HTTP, not 100k origin WebSockets. The STOMP path remains for a capped hybrid cohort. It is not a current capacity claim.

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

- Paid Redis/Neon, region migration, service consolidation
- Analytics/replay production SLOs (no live ES/R2 yet)
