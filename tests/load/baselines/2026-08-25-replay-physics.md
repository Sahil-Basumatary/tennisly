# 2026-08-25 replay physics frames/s

This is in-process generated replay frames. It is not HTTP, not Postgres, and not assembler-only disguised as the full pipeline.

Golden SHA-256 values were captured on the unoptimised array-of-structures engine, then re-checked after allocation cuts, structure-of-arrays path buffers, and parallel point generation. Bit-identical. Fixture: [replay-golden-sha256.txt](replay-golden-sha256.txt).

## Environment

- date: 2026-08-25T21:42Z
- hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- runtime: Temurin 21.0.10, JMH 1.37
- command: `make jmh-replay` → `./scripts/replay-physics-bench.sh`
- forks: 1 isolated JVM
- jvm: `-Xms512m -Xmx512m`
- warmup: 3 × 1 s
- measurement: 5 × 1 s, `Mode.Throughput`
- profiler: JMH `GCProfiler`
- engine: fps=60, step=0.002, max_flight=6.0, solver_iters=48, tolerance=0.05
- seeds: point `123456789`, match `0xC0FFEE1234`
- golden: 363 point frames, 4341 match frames

## Results

| Operation | Throughput | Derived frames/s | alloc/op | GC time (sum) |
|---|---:|---:|---:|---:|
| RK4 step | 6,512,571 ops/s | n/a | 384 B | 32 ms |
| Launch solve | 196.05 ops/s | n/a | 33,403 B | ≈ 0 |
| Assembler-only (one production-shaped point) | 84,751 points/s | **30,764,462 assembler frames/s** | 77,240 B | 90 ms |
| Full solver-to-frame (one point) | **40.208 points/s** | **14,596 full-pipeline frames/s** | 1,045,975 B | ≈ 0 |
| Full solver-to-frame (12-point match) | 3.434 matches/s | 14,905 full-pipeline frames/s | 12,515,704 B | 1 ms |

Headline metric is the full solver-to-frame point pipeline: **14,596 frames/s**. Assembler-only is interpolation of an already-solved path and is a separate claim.

The 250k / 500k / 1M full-pipeline frames/s ladder was not reached on this laptop. Accuracy was not relaxed to force a pass.

## Correctness

- point frames SHA-256 `85aa9d59c8eb0def096284198c61853c0673af4a6c0d8b4ba927fc5555e32ab5`
- point shots SHA-256 `ce4c9fcfe100215c794ee8b286e084b833dbd8d0a2ba5a3e79bf40ee4b638c75`
- match frames SHA-256 `7597e358ba23f43b629423f48b36c95124f30e9336eabf86402bcd08fc9114ce`

Regression floor: `fullPointPipeline` ≥ 10 points/s (`BenchmarkRunner.gateScale`).

## Limitations

One JMH fork. Throughput mode, not sample-time p99. Single laptop JVM. Does not prove HTTP replay download, stored-artifact serving, or a later engine-version bump.
