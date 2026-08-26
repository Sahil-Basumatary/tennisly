# Load baselines

Commit reviewed summary tables here after a warm production or local run.

First measured HTTP row: [2026-08-22-smoke-live.md](2026-08-22-smoke-live.md) (live catalogue k6 misses the public-gateway SLO).
Forked JMH row: [2026-08-22-jmh-forked.md](2026-08-22-jmh-forked.md) (in-process p99 under 1 ms, 2 JVM forks).
CPU throughput before/after: [2026-08-22-cpu-decision-throughput.md](2026-08-22-cpu-decision-throughput.md) (12.40M operations/s and 79.4% lower allocation; not durable TPS).
Durable HTTP write before/after: [2026-08-22-durable-match-write.md](2026-08-22-durable-match-write.md) (270.63 Postgres commits/s, p99 150.51 ms, zero durability mismatches).
Live WebSocket delivery: [2026-08-22-live-websocket.md](2026-08-22-live-websocket.md) (two-node hot match: 35 ms p99 and zero gaps; two-node eight-topic run: 109 ms p99 failed SLO; not a 100k claim).
Staged live capacity: [2026-08-22-live-capacity.md](2026-08-22-live-capacity.md) (backpressure, replay, node-kill; unrecovered gaps 0; not a 100k claim).
Replay physics frames: [2026-08-25-replay-physics.md](2026-08-25-replay-physics.md) (historical single-fork: 14,596 full-pipeline frames/s; assembler-only is a separate 30.8M frames/s claim).
Archive tape: [2026-08-25-archive-tape.md](2026-08-25-archive-tape.md) (historical single-fork: 36.2 million-event tapes/s at 8 workers; not Postgres).
Durable HTTP write (JDBC hot path): [2026-08-25-durable-match-write.md](2026-08-25-durable-match-write.md) (historical single-run: 638.67 atomic commits/s, p99 41.56 ms, zero durability mismatches).
Bulk historical ingest: [2026-08-25-bulk-ingest.md](2026-08-25-bulk-ingest.md) (historical single-run: 5,458 transactional batch points/s; 20,991 COPY rows/s for 1,000,000 source rows).
Replay engine 2.0 goldens: [replay-golden-sha256.txt](replay-golden-sha256.txt). Engine 1.0 hashes remain in [replay-golden-sha256-v1.txt](replay-golden-sha256-v1.txt).
Multi-fork v2 evidence: [2026-08-26-v2-evidence.md](2026-08-26-v2-evidence.md) (generated from session JSON). Local floors: [v2-floors.json](v2-floors.json) (20% slack, not PR CI).
Near-live HTTP cache-collapse (recovery stampede): [2026-08-26-http-live-cache.md](2026-08-26-http-live-cache.md) (local CDN stand-in, 2s mock cadence, recover-on-join; 100 then 1k viewers; not Vercel Hobby and not a 100k claim).
Near-live HTTP recovery-fixed: [2026-08-26-http-live-recovery.md](2026-08-26-http-live-recovery.md) (20s cadence, skip join/one-step `/events`, real match-service stack; 0 recovery origin during play).
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
