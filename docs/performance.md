# Performance (Week 31)

Measurable wins on the hottest paths — not a CDN rewrite.

## What changed

| Area | Change | Why |
|---|---|---|
| Match list | `@EntityGraph(players)` + batched point counts | Was 1+2N queries (players + full points collection just for `.size()`) |
| Match get | Prefer Redis `live-match:{id}` snapshot, else DB + `countByMatchId` | Closes write-only live cache; avoids loading point tapes for a count |
| Match indexes | `V3__match_list_indexes.sql` `(status, scheduled_at)` and `(tournament_id, status, scheduled_at)` | Filtered catalogue sorts stay cheap as rows grow |
| Gateway API keys | Redis cache of successful validates (`apikey:valid:{sha256}`, TTL 30s) | Every `/api/v1/**` hit was a user-service round-trip |
| Web rankings BFF | `Cache-Control: private, max-age=30, stale-while-revalidate=60` | Backend already Redis-cached; BFF was forcing `no-store` |
| Match centre | `next/dynamic` court panel + `optimizePackageImports` for Babylon/Visx/Framer | Keep heavy 3D/chart code off the critical path |

## Security note (API-key cache)

Revoke can lag up to **TTL seconds** (default 30). Keep TTL short. Invalidate-on-revoke via shared Redis delete is a follow-up if abuse shows up.

## Load smoke

```bash
export API_KEY=tly_live_...
export BASE_URL=http://localhost:8080
make load-smoke
# or: k6 run tests/load/public-api-smoke.js
```

Covers `players`, `rankings`, and `matches` list. Threshold: p95 < 800ms, error rate < 5%.

## Explicitly deferred

- Cloudflare / edge CDN (not wired)
- Analytics Redis / ES aggregation rewrite
- Full Core Web Vitals dashboarding in CI
