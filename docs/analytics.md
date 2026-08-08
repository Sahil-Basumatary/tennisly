# Analytics platform

Tape-provable match analytics: Kafka completion events and a resumable backfill write Elasticsearch read models; the web app reads them through a Next.js BFF.

## What is tape-provable

Only metrics the point ledger can prove:

- points won
- service points won
- breaks (returner wins the game — scoreboard points reset to `0-0`)
- surfaces, timestamps, tournament key, head-to-head meetings

Synthesized trajectory / shot labels from replay-service are **out of scope** for analytics.

## Local stack

| Piece | Default host port | Notes |
|-------|-------------------|--------|
| Elasticsearch | `19200` | single-node, security off |
| analytics-service | `18086` | Eureka + Kafka + Postgres `tennisly_analytics` + ES |
| Web BFF | via `WEB_PORT` | `ANALYTICS_SERVICE_URL` → analytics-service |

`make up` starts Elasticsearch with the infra profile and boots analytics-service. If `.run/ports.env` predates analytics, reallocate:

```bash
ALLOCATE_FORCE=1 make ports
make up
```

Postgres init scripts only run on a **new** volume. Existing volumes miss `tennisly_analytics` until `scripts/ensure-databases.sh` runs (invoked from `dev-up.sh` after Postgres is healthy). You can also create the DB manually:

```bash
docker exec tennisly-postgres psql -U tennisly -d postgres -c "CREATE DATABASE tennisly_analytics;"
```

## Indices and aliases

| Alias (writes + reads) | Versioned index |
|------------------------|-----------------|
| `tennisly-match-analytics` | `tennisly-match-analytics-v1` |
| `tennisly-player-match` | `tennisly-player-match-v1` |

Bootstrap creates the index + alias on startup if missing. Document ids:

- match doc: `{matchId}`
- player-match doc: `{playerId}_{matchId}`

Re-indexing the same match replaces those ids (idempotent).

## Rebuild / backfill

Internal only (not on the API gateway):

```bash
# start
curl -s -X POST http://localhost:18086/internal/analytics/reindex

# poll
curl -s http://localhost:18086/internal/analytics/reindex/{jobId}
```

The job pages `GET /internal/matches/completed?cursor=&limit=` on match-service, reconciles each match (fetch match + points → aggregate → index), and stores cursor progress on `analytics_reindex_jobs`.

Kafka path: `MATCH_STATUS_CHANGED` with `COMPLETED`, and `MATCH_POINT_RECORDED`, with receipts in `analytics_ingest_receipts` keyed by `eventId`.

## Public query API

Anonymous GETs (gateway permits `GET /api/analytics/**` except views):

| Path | Purpose |
|------|---------|
| `/api/analytics/matches/{id}` | Match tape metrics |
| `/api/analytics/players/{id}` | Summary + recent matches |
| `/api/analytics/players/{id}/trends` | Chronological trend points |
| `/api/analytics/compare?playerA=&playerB=` | Head-to-head |
| `/api/analytics/tournaments/{tournamentKey}` | URL-encode `\|` in the key |
| `.../export.csv` | CSV download |
| `.../matches/{id}/report` | Print-oriented JSON |

Bounds: page size clamp, max 366-day range, export row caps.

## Saved views

`/api/analytics/views/**` requires JWT at the gateway. Analytics-service trusts only gateway-forwarded `X-User-Id` / `X-Org-Id` (spoofed headers are stripped at the gateway). Local BFF talks to analytics-service directly and sets `X-User-Id` from Clerk `userId`.

## Web UI

Routes under `/analytics` (Visx charts). Saved views page `/analytics/views` is Clerk-protected. Set in `.env.local`:

```bash
ANALYTICS_SERVICE_URL=http://localhost:18086
```

## Smoke checklist

1. ES cluster green: `curl -s http://localhost:19200/_cluster/health`
2. Aliases present: `curl -s 'http://localhost:19200/_cat/aliases?v'`
3. Reindex completes with `processedCount` matching completed matches
4. Match GET returns home/away metrics (not 500)
5. Views without identity → 401; with `X-User-Id` → 200
6. Open `/analytics` in the browser after web is up
