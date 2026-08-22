# 2026-08-22 smoke — first measured row

Not a pass against the public-gateway SLO. Errors are 0%; latency is not.

## Environment

date: 2026-08-22T09:50Z
origin: https://api-gateway-lryh.onrender.com
client: UK laptop, arm64, Temurin 21.0.10
scenario: k6 `smoke` (1 VU, 10s warmup + 30s measured)
warmup: dedicated k6 stage; custom trends still include warmup samples (13 catalogue loops, ~4 warm + 9 measured)
error_rate: 0 / 39 HTTP requests (all 200)
rps: 0.89
dataset: live catalogue, `gender=MALE`, `status=IN_PROGRESS`
limitations: client UK → Cloudflare → Render (region not pinned; often US) → Neon (examples `eu-west-2`). Live responses are **unbounded lists** (pagination not deployed). JMH in this file is the discarded `forks=0` run; use [2026-08-22-jmh-forked.md](2026-08-22-jmh-forked.md). No Lighthouse, no `/api/v1`, no soak/burst.

## Public gateway (k6 smoke)

| Metric | p50 | p95 | p99 | SLO (p95 / p99) |
|---|---:|---:|---:|---|
| all `http_req_duration` | 682 ms | 1.84 s | 1.97 s | 250 ms / 500 ms — **miss** |
| players `GET /api/tennis/players?gender=MALE` | 682 ms | 937 ms | 1.15 s | **miss** |
| rankings `GET /api/tennis/rankings?gender=MALE&type=SINGLES` | 288 ms | 434 ms | 463 ms | p95 **miss**, p99 **hit** |
| matches `GET /api/matches?status=IN_PROGRESS` | 1.41 s | 1.96 s | 1.98 s | **miss** |

Single-shot curl after a health ping (not percentiles): health TTFB 409 ms; players TTFB 469 ms / total 734 ms; rankings TTFB 848 ms / total 1.10 s; matches TTFB 1.77 s / total 1.94 s.

Live payload sizes on a follow-up GET: players 539 KB (1450 rows), rankings 92 KB (422 rows), matches 263 KB (230 rows).

## In-process JMH (local, forks=0, superseded)

Superseded by [2026-08-22-jmh-forked.md](2026-08-22-jmh-forked.md). Kept so the debug run is not confused with the forked gate.

## Not measured this run

Same-region Redis, indexed Neon from inside Render, Vercel LCP/INP/CLS, WebSocket deltas, `/api/v1` with an API key, burst, soak, JFR.
