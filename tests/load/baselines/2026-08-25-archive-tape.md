# 2026-08-25 in-memory archive processor

This is deterministic tape processing of generated events. It is not HTTP, not Postgres commits, and not Elasticsearch indexing.

## Environment

- date: 2026-08-25T21:43Z
- hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- runtime: Temurin 21.0.10, JMH 1.37
- command: `JMH_HEAP=1024m make jmh-archive` → `./scripts/archive-tape-bench.sh`
- forks: 1 isolated JVM
- jvm: `-Xms1024m -Xmx1024m`
- warmup: 3 × 1 s
- measurement: 5 × 1 s, `Mode.Throughput`
- profiler: JMH `GCProfiler`
- dataset: `ArchiveEventGenerator.Spec.million()` — 1000 matches, 1,000,000 unique events, 2000 duplicates, 1000 gaps, seed `0xA11CE5EED`

## Results

| Workers | Million-event tapes/s | Unique events/s | alloc/op | GC time (sum) |
|---|---:|---:|---:|---:|
| 1 | 13.086 | 13,086,000 | 53.4 MB | 25 ms |
| 8 | **36.176** | **36,176,000** | 77.1 MB | 50 ms |

Count conservation `accepted + duplicates == sourceRows` held in unit tests. SHA-256 fingerprints matched at 1 and 10 workers. Gaps were recorded; missing events were not invented.

Regression floor: `processMillionEvents` ≥ 0.1 tapes/s (100k events/s). This run is above 1M events/s on both thread counts.

## Limitations

One JMH fork. Generated tape, not a committed 150–250 MB fixture. Does not prove durable ingest, SQL, or Elasticsearch.
