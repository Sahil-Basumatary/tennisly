# Near-live HTTP recovery-fixed (2026-08-26)

This is a **CDN viewer RPS vs origin RPS** measurement after skipping join and one-step `/events` recovery. It is not WebSocket capacity, not Vercel Hobby, and not 100k concurrent viewers.

The earlier 2-second mock cadence with recover-on-join is kept as [2026-08-26-http-live-cache.md](2026-08-26-http-live-cache.md). Do not overwrite that file.

date: 2026-08-26
hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
k6: Grafana k6 v2.1.0, ramping VUs, cursor poll ~3s + jitter, ticker every third poll
point cadence: 20s (Python mock) / 15s (real match-service writer)
limitations: local edge stand-in, not Vercel CDN; Hobby quotas were not used

## What changed

Viewers poll `GET /api/matches/{id}/cursor` with `If-None-Match`. They call `/events` only when `liveSequence > cursor + 1` **and** `cursor > 0`. Join (`cursor === 0`) and a single-sequence advance adopt the compact cursor. `/events` stays `private, no-store` at the CDN; identical pages coalesce in the BFF (`singleFlight`) and in Redis for 1s.

## Python origin + in-process edge (`make load-http-live`)

origin: `scripts/live-http-edge.py` mock + edge
command: `HTTP_LIVE_CLIENTS=100|1000 HTTP_LIVE_HOLD_S=25 HTTP_LIVE_POINT_INTERVAL_S=20 make load-http-live`

| Stage | Viewer reqs | Score-cache hits | Origin fetches | `/events` fetches | Unrecovered gaps | Errors |
|---|---:|---:|---:|---:|---:|---:|
| 100 VUs, 3s ramp + 25s hold (`k6-live-http-100-20260826T110313Z`) | 2,644 | 99.16% (2,622 / 2,644) | 22 | 0 | 0 | 0 |
| 1,000 VUs, 5s ramp + 25s hold (`k6-live-http-1000-20260826T110407Z`) | 14,496 | 99.83% (14,472 / 14,496) | 24 | 0 | 0 | 0 |

Cache-collapse (all requests were cacheable score/ticker documents):

- 100 viewers: 2,644 / 22 = **120×**
- 1,000 viewers: 14,496 / 24 = **604×**

Recovery rate (event fetches / cursor polls): **0%** at both stages. Duplicate applications: **0**.

### Latency (Python stand-in)

| Stage | p50 | p95 | p99 | max |
|---|---:|---:|---:|---:|
| 100 VUs | 0.88 ms | 5.82 ms | 11.5 ms | 156 ms |
| 1,000 VUs | 0.50 ms | 38.6 ms | 55.9 ms | 75.0 ms |

Compare with the stampede 1,000-VU row: p99 293 ms, 3,568 `/events` origin fetches. Removing the recovery stampede is what dropped the tail, not a faster CPU.

## Real stack (`make load-http-live-stack`)

origin: match-service on `:18096` + local Postgres `15432` + Redis `16379`, Python edge on `:18097`
writer: one VU, `POST /points` every 15s, HTTP 201 only after the atomic commit
command: `HTTP_LIVE_CLIENTS=100|1000 POINT_INTERVAL_MS=15000 HTTP_LIVE_HOLD_S=30 make load-http-live-stack`

| Stage | Cacheable viewer reqs | Score-cache hits | Edge→origin fetches | Recovery rate | Unrecovered gaps | Errors |
|---|---:|---:|---:|---:|---:|---:|
| 100 VUs (`k6-live-http-real-100-20260826T110507Z`) | 3,554 | 99.29% (3,529 / 3,554) | 25 | 0% | 0 | 0 |
| 1,000 VUs (`k6-live-http-real-1000-20260826T110625Z`) | 16,722 | 99.83% (16,694 / 16,722) | 28 | 0% | 0 | 0 |

Collapse: **142×** at 100 VUs (3,554 / 25), **597×** at 1,000 VUs (16,722 / 28).

### Viewer latency (excludes writer POSTs)

| Stage | p50 | p95 | p99 | max |
|---|---:|---:|---:|---:|
| 100 VUs | 1.86 ms | 7.47 ms | 14.6 ms | 56.5 ms |
| 1,000 VUs | 0.40 ms | 22.4 ms | 64.4 ms | 175 ms |

All-request HTTP p99 includes the writer's durable commit (max 374 ms at 100 VUs, 533 ms at 1,000 VUs). That is Postgres write time, not viewer score-poll time.

## Gates

k6 fails the run unless:

- `http_req_failed < 1%`
- unrecovered gaps = 0
- duplicate applications = 0
- recovery rate `< 0.1%` of cursor polls
- score-cache hit rate `> 98%` at 100 VUs and `> 99.7%` at 1,000 VUs

99.83% at 1,000 VUs is the **s-maxage=2 / 3s poll** ceiling (about one origin revalidate per document per TTL vs hundreds of viewer RPS). 99.9% is the 10k-viewer arithmetic at the same TTLs. Do not claim 99.9% from a 1k-laptop run.

## What this does not prove

- Not tennisly.tv / Vercel Hobby. `ALLOW_VERCEL_LIVE_HTTP=1` is required to point k6 at a remote URL; it was not set.
- Not 100k concurrent viewers. The local harness refuses more than 2,000 VUs.
- Not Render origin RPS under production TLS.
- Not court-replay throughput. Sealed point artifacts are a separate cache policy.

Measured ceiling today: **1,000 local HTTP viewers** on both the Python stand-in and match-service+Postgres, 0 errors, 0 unrecovered gaps, 0 recovery origin during normal play. 100k remains an architecture, not a result.
