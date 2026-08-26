# 2026-08-25 bulk historical ingest

This is not atomic four-row HTTP point TPS. Batch points/s and COPY rows/s are labelled separately.

## Environment

- date: 2026-08-25T21:45Z
- hardware: Apple M1 Pro, 10 cores, 16 GB, darwin arm64
- runtime: Temurin 21.0.10
- match-service on the host; Postgres 16.11 Alpine and Redis 7 in local Docker
- ports: Postgres 15432, Redis 16379, match-service 18094
- durability: `synchronous_commit=on`; `fsync` not disabled
- observed Postgres: `wal_compression=off`, `shared_buffers=128MB`
- command: `BATCH_POINTS=2000 BATCH_SIZE=200 COPY_ROWS=1000000 make load-bulk`
- synthetic COPY is gated by `ARCHIVE_SYNTHETIC_INGEST=true` (default false)

## Transactional batch (internal `POST /internal/matches/{id}/points/batch`)

Ordered multi-point inserts, max 1000 points per request, idempotency key, all-or-nothing validation.

- accepted: 2,000
- seconds: 0.366
- **5,457.90 points/s**

## COPY / staging (internal synthetic historical ingest)

PostgreSQL `COPY` into `match_archive_staging`, then `INSERT ... ON CONFLICT (match_id, sequence_number) DO NOTHING`.

- source rows: 1,000,000
- accepted rows: 1,000,000
- duplicate rows: 0
- seconds: 47.641
- **20,990.53 rows/s**
- distinct `(match_id, sequence_number)`: 1,000,000
- `matches.point_count`: 1,000,000

Restart promote of a completed job in integration tests accepted 0 additional rows and counted the previous rows as duplicates.

## Limitations

Internal archive API, not the public single-point endpoint. Synthetic COPY is disabled unless `ARCHIVE_SYNTHETIC_INGEST` is set. Do not compare 20,990 COPY rows/s with 638.67 atomic commits/s.
