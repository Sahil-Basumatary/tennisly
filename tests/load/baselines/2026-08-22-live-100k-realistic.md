# 2026-08-22 live 100k realistic

Attempted staged proof of 100,000 concurrent STOMP subscribers on eight match topics. **100k was not reached.** The measured ceiling on this laptop is 100 ramped clients.

## Environment

- Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- Temurin 21.0.10, k6 v2.1.0
- match-service × 1, k6 on the host
- Postgres 16.11 and Redis 7 on local container ports
- Kafka and upstream ingestion disabled
- no kubeconfig, Docker CLI, or cloud credentials on this Mac ([preflight](2026-08-22-live-100k-preflight.md))
- ramped subscribers (`SUBSCRIBER_RAMP`), writers start after the ramp
- latency is `now - commitObservedAt` on in-order live frames

## Stages

| Stage | Clients | p50 | p95 | p99 | Subscribed | Unrecovered | Result |
|---|---:|---:|---:|---:|---:|---:|---|
| 100 realistic | 100 | 8 ms | 23 ms | 30 ms | 100/100 | 0 | pass |
| 1,000 realistic | 1,000 | 27 ms | 708 ms | 1,661 ms | 456/1000 | 0 | fail p99 and incomplete plateau |
| 10,000 | — | — | — | — | — | — | not run; previous gate failed |
| 25,000 / 50,000 / 100,000 | — | — | — | — | — | — | not run; no cluster |

### 100 (pass)

- 5,400 live messages, 8 topics, 1 node
- connect failures, duplicates, malformed frames, unrecovered gaps: 0
- durability: 488 event-log rows = 488 outbox rows, 0 sequence violations
- server active-session gauge returned to 0 after the run

### 1,000 (fail)

- correctness still held: unrecovered 0, duplicates 0, malformed 0, write failures 0
- only 456 subscriber functions completed; 773 VUs were interrupted during graceful stop
- connect p99 805 ms; delivery p99 1.66 s (gate 50 ms)
- publish_max 725 ms — one JVM, k6, Postgres and Redis sharing 10 cores
- this is a laptop topology limit, not a passing 1k result

## Cluster 100k

Not executed. `APPROVE_SCALE_PROVISION` was not set, `MATCH_SERVICE_IMAGE` is unset, and `kubectl` has no context. Manifests and the runner are in `infrastructure/kubernetes/live-100k/` and `scripts/match-live-scale.sh`.

## Reproduction

```bash
WS_MODE=realistic WS_CLIENTS=100 SUBSCRIBER_RAMP=5s WRITER_START=5s WS_HOLD_MS=15000 WS_DURATION=15s make load-websocket
# 100k requires a cluster:
# APPROVE_SCALE_PROVISION=true MATCH_SERVICE_IMAGE=...@sha256:... PROFILE=realistic make load-live-scale
```

## Limitations

- One load generator and one match-service JVM on 16 GiB.
- Per-worker k6 percentiles were used locally because there is a single k6 process; distributed runs must use Prometheus native histograms.
- Redis Pub/Sub is at-most-once; Postgres replay is the recovery plane.
