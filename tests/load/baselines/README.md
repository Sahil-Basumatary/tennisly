# Load baselines

Commit reviewed summary tables here after a warm production or local run.

First measured HTTP row: [2026-08-22-smoke-live.md](2026-08-22-smoke-live.md) (live catalogue k6 misses the public-gateway SLO).
Forked JMH row: [2026-08-22-jmh-forked.md](2026-08-22-jmh-forked.md) (in-process p99 under 1 ms, 2 JVM forks).
CPU throughput before/after: [2026-08-22-cpu-decision-throughput.md](2026-08-22-cpu-decision-throughput.md) (12.40M operations/s and 79.4% lower allocation; not durable TPS).
Durable HTTP write before/after: [2026-08-22-durable-match-write.md](2026-08-22-durable-match-write.md) (270.63 Postgres commits/s, p99 150.51 ms, zero durability mismatches).
Live WebSocket delivery: [2026-08-22-live-websocket.md](2026-08-22-live-websocket.md) (two-node hot match: 35 ms p99 and zero gaps; two-node eight-topic run: 109 ms p99 failed SLO; not a 100k claim).
Staged live capacity: [2026-08-22-live-capacity.md](2026-08-22-live-capacity.md) (backpressure, replay, node-kill; unrecovered gaps 0; not a 100k claim).
100k preflight: [2026-08-22-live-100k-preflight.md](2026-08-22-live-100k-preflight.md) (kubectl installed; no kubeconfig/cluster on the laptop; not a 100k run).
100k realistic staging: [2026-08-22-live-100k-realistic.md](2026-08-22-live-100k-realistic.md) (100-client pass, 1k-client fail; 100k not run).
100k hot: [2026-08-22-live-100k-hot.md](2026-08-22-live-100k-hot.md) (not run; blocked on realistic 100k).

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
