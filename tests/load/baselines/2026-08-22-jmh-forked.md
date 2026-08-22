# 2026-08-22 JMH — forked uber-jar

Defensible in-process p99. Not a public HTTP claim.

## Environment

date: 2026-08-22T10:24Z
hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
runtime: Temurin 21.0.10, JMH 1.37
command: `./scripts/jmh-run.sh` → `java -jar services/performance-benchmarks/target/benchmarks.jar`
forks: 2 isolated JVMs (`ForkedMain` on the shade classpath)
jvm: `-Xms256m -Xmx256m`
warmup: 3 × 1 s
measurement: 5 × 1 s, `Mode.SampleTime`
samples: 230k–298k ops per benchmark
profiler: JMH `GCProfiler` (alloc/op)
gate: p99 < 1 ms (`BenchmarkRunner`)

## Results

| Benchmark | p50 | p95 | p99 | alloc/op | 1 ms p99 |
|---|---:|---:|---:|---:|---|
| `apiKeyHash` | 7.74 µs | 11.5 µs | **61.3 µs** | 17404 B | **hit** |
| `matchStateValidation` | 41 ns | 42 ns | **208 ns** | ~0 B | **hit** |
| `rateLimitDecision` | 41 ns | 42 ns | **167 ns** | 40 B | **hit** |
| `tapeAggregator` | 83 ns | 125 ns | **333 ns** | 504 B | **hit** |

`apiKeyHash` p99.9 is 512 µs; p99.99 is 3.5 ms. Those tails are GC, not the SHA-256 itself. The gate is p99, which still clears 1 ms with ~16× margin.

## What this does and does not prove

Does: selected Java hot paths stay under 1 ms p99 in a forked JVM on this hardware, with a CI gate that rejects empty results.

Does not: browser, Vercel, Render, Redis, Neon, or k6 public-gateway latency. Those remain the 2026-08-22 smoke row (p95 1.84 s).

The previous `forks=0` / `exec:java` numbers are debug-only and are not this row.
