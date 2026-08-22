# 2026-08-22 CPU decision throughput

This is an in-process CPU benchmark, not HTTP or durable database TPS.

## Operation definition

One `pointDecisionPipeline` operation:

1. validates that an in-progress match can record a point;
2. validates the transition to completed;
3. calculates an in-memory rate-limit decision;
4. aggregates eight point summaries into match-side metrics.

It does not open a socket, authenticate a user, commit Postgres, publish Kafka, or execute the outbox.

## Protocol

- Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- Temurin 21.0.10, JMH 1.37
- one benchmark thread
- three isolated JVM forks
- five 1-second warmups per fork
- ten 1-second measurements per fork
- 256 MiB fixed heap
- JMH GC profiler

## Before

The aggregator used streams, two mutable buckets and temporary side lookup objects.

- throughput: 9.47M operations/s ± 1.63M (99.9% CI)
- allocation: 544 B/operation
- GC collections: 975 across 30 measurement iterations
- GC time: 845 ms

## After

The aggregator resolves sides in one pass, counts into local primitives and allocates only the returned metric records.

- throughput: 12.40M operations/s ± 3.03M (99.9% CI)
- allocation: 112 B/operation
- GC collections: 263 across 30 measurement iterations
- GC time: 260 ms

Observed change:

- 30.9% higher mean throughput
- 79.4% less allocation per operation
- 73.0% fewer GC collections
- 69.2% less GC time

The throughput confidence intervals overlap because this laptop was not CPU-isolated. Allocation per operation is stable to within 0.001 B and is the strongest regression metric. Do not present 30.9% as a laboratory-grade causal estimate.

## Regression gate

The normal two-fork `make jmh` run measured 9.25M operations/s after optimization and passed:

- point-decision floor: 1M operations/s
- selected latency hot paths: p99 below 1 ms
- non-empty result enforcement

Raw JMH JSON remains under `.run/performance/` and is intentionally gitignored.
