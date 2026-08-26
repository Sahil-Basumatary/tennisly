# Near-live HTTP cache-collapse (2026-08-26)

**Recovery-stampede dataset.** Keep this file. The mock origin advanced `liveSequence` every **2 seconds**, and viewers recovered whenever `sequence > cursor + 1`, including first join after sequence had moved. That is why 3,568 of 3,582 origin fetches at 1,000 VUs were uncached `/events`.

Corrected evidence (20s cadence, skip join and one-step recovery, real match-service stack): [2026-08-26-http-live-recovery.md](2026-08-26-http-live-recovery.md).

This is a **CDN viewer RPS vs origin RPS** measurement. It is not WebSocket capacity, not Vercel Hobby, and not 100k concurrent viewers.

date: 2026-08-26
origin: local Python origin + in-process edge cache (`scripts/live-http-edge.py`)
hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
command: `make load-http-live` (`HTTP_LIVE_CLIENTS=100` then `1000`)
k6: Grafana k6, ramping VUs, cursor poll ~3s + jitter, ticker every third poll
limitations: Python GIL stand-in, not Vercel CDN; event recovery (`/events`) is intentionally uncached; Hobby quotas were not used

## What was measured

Viewers poll `GET /api/matches/{id}/cursor` with `If-None-Match`. When `liveSequence` jumps more than one, they recover through `GET /events?afterSequence=`, which must not be CDN-cached. The ticker is a second shared document.

| Stage | Viewer reqs | Cacheable hit rate | Score/ticker origin misses | Uncached event fetches | Origin total | Unrecovered gaps | Errors |
|---|---:|---:|---:|---:|---:|---:|---:|
| 100 VUs, 3s ramp + 12s hold | 1,137 | 98.47% (775 / 787) | 12 | 350 | 362 | 0 | 0 |
| 1,000 VUs, 5s ramp + 12s hold | 11,487 | 99.82% (7,905 / 7,919) | 14 | 3,568 | 3,582 | 0 | 0 |

Cache-collapse on the **score documents** (ticker + cursor, excluding recovery):

- 100 viewers: 787 / 12 = **65.6×**
- 1,000 viewers: 7,919 / 14 = **565×**

That ratio is why 100k WebSockets are the wrong default on this budget. It is not a 100k run.

## Latency (local stand-in)

| Stage | p50 | p95 | p99 | max | notes |
|---|---:|---:|---:|---:|---|
| 100 VUs (`k6-live-http-100-20260826T002350Z`) | 0.68 ms | 14.8 ms | not in that export; **max 47.3 ms** | 47.3 ms | first passing run |
| 100 VUs p99 export (`...T002555Z`) | 1.14 ms | 486 ms | 744 ms | 1.00 s | later pass; Python tail, recorded |
| 1,000 VUs (`k6-live-http-1000-20260826T002614Z`) | 0.85 ms | 74.5 ms | 293 ms | 574 ms | p99 is the GIL stand-in, not Vercel |

Stale-while-revalidate age on HITs: p95 1–2 s, matching `s-maxage=2`.

Bytes (1,000 VUs, ~20 s): **1.59 MB** received for 11,487 requests (~139 bytes/request including 304s and recovery pages).

## Correctness

- Unrecovered sequence gaps: **0** at 100 and 1,000
- HTTP errors: **0** on the published rows (an earlier 100-VU pass without listen-backlog coalescing had 14 dial timeouts; that is why the edge coalesces origin fetches)
- Duplicate applications are counted; none blocked the run

## What this does not prove

- Not tennisly.tv / Vercel Hobby. `ALLOW_VERCEL_LIVE_HTTP=1` is required to point k6 at a remote URL; it was not set.
- Not 100k concurrent viewers. The local harness refuses more than 2,000 VUs.
- Not Render origin RPS under production TLS. Origin here is a Python mock with coalesced GETs.
- Not court-replay throughput. Sealed point artifacts are a separate cache policy.

## Vercel Hobby budget (architecture, not a run)

Included usage: 100 GB Fast Data Transfer, 10 GB Fast Origin Transfer, 1M function invocations, 4 CPU-hrs. Hitting those pauses the project.

If 100k browsers polled a ~200 byte cursor every 3 s with **no** CDN, that is about 33k origin RPS and hundreds of GB/day. With a 500× score-document collapse and 304s, origin RPS falls to the cache-miss cadence (~one revalidate per document per `s-maxage`) plus uncached recovery. Court tape for all 100k would still blow Hobby bandwidth; court stays opt-in.

Measured ceiling today: **1,000 local HTTP viewers**, 0 errors, 0 unrecovered gaps, 565× score-document collapse. 100k remains an architecture, not a result.
