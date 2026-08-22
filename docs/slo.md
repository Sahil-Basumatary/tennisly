# Service level objectives

Layered latency SLOs for the paid public path. Sub-1 ms is **not** an end-to-end HTTP target.

## Scope

In: Vercel web, `api-gateway`, `tennis-data-service`, `match-service`, Render Redis, Neon.

Out of production gates until always-on: auth, user, notification, analytics, replay.

Cold starts and deploys are a **separate dataset**. They must not enter warm p50/p95/p99.

## Gates

| Layer | p50 | p95 | p99 | Errors |
|---|---:|---:|---:|---:|
| In-process JMH (selected hot paths) | — | — | < 1 ms | n/a |
| Same-region Redis op | — | < 3 ms | < 10 ms | report misses separately |
| Indexed Neon query | — | < 25 ms | < 75 ms | — |
| Warm backend endpoint | — | < 100 ms | < 200 ms | < 0.1% |
| Warm public gateway (UK) | — | < 250 ms | < 500 ms | < 0.1% |
| Web (home, catalogue, match centre) | LCP < 2.5 s | INP < 200 ms | CLS < 0.1 | — |

Success is **HTTP 200 only**. 401/403/429 count as failures for these gates.

## Local live delivery (lab)

Not a public-internet SLO. Measured from `commitObservedAt` to STOMP client receipt.

| Layer | p50 | p95 | p99 | Correctness |
|---|---:|---:|---:|---|
| Healthy live WebSocket | report | report | < 50 ms | unrecovered gaps = 0 |
| Failover reconnect | report | report | < 80 ms | unrecovered gaps = 0 |

Slow clients are excluded from the healthy p99 sample and may be disconnected. Evidence: [tests/load/baselines/2026-08-22-live-capacity.md](../tests/load/baselines/2026-08-22-live-capacity.md).

## How to measure

1. Warm the three Starter services with a dedicated warm-up stage.
2. Run `SCENARIO=smoke ./scripts/k6-load.sh` against the gateway origin.
3. Record hardware, region, dataset, concurrency, warm-up, sample size.
4. Store JSON under `.run/performance/` (gitignored). Commit summaries into `tests/load/baselines/` only after a reviewed run.
5. Forked JMH: `make jmh` (shaded `benchmarks.jar`, not `exec:java`).

`/api/v1` needs a live API key and warm user-service. Use `tests/load/public-api-v1.js` separately.

## Honesty

Browser → Vercel → Render RTT cannot be sub-1 ms on the public internet. Sub-1 ms claims are JMH in-process p99 with published hardware and allocation data.
