# Load baselines

Commit reviewed summary tables here after a warm production or local run.

First measured HTTP row: [2026-08-22-smoke-live.md](2026-08-22-smoke-live.md) (live catalogue k6 misses the public-gateway SLO).
Forked JMH row: [2026-08-22-jmh-forked.md](2026-08-22-jmh-forked.md) (in-process p99 under 1 ms, 2 JVM forks).
CPU throughput before/after: [2026-08-22-cpu-decision-throughput.md](2026-08-22-cpu-decision-throughput.md) (12.40M operations/s and 79.4% lower allocation; not durable TPS).
Durable HTTP write before/after: [2026-08-22-durable-match-write.md](2026-08-22-durable-match-write.md) (270.63 Postgres commits/s, p99 150.51 ms, zero durability mismatches).
Live WebSocket delivery: [2026-08-22-live-websocket.md](2026-08-22-live-websocket.md) (100 clients, 28–33 ms p99, zero gaps or duplicates; not a 100k claim).

Do not commit raw k6 JSON from `.run/performance/` (gitignored).

Template:

```
date:
origin:
scenario:
warmup:
p50_ms:
p95_ms:
p99_ms:
error_rate:
notes:
```
