# Phase 8 deploy — Vercel (web) + Render (API)

Replaces the plan’s EKS/Helm path with a portfolio-honest cloud cut:
**Next.js on Vercel**, **Spring services on Render**.

## Topology (Phase 8a — first public demo)

```
Browser
  → Vercel (@tennisly/web)  — Clerk + Next BFF /api/*
       → Render tennis-data-service (HTTPS)
       → Render match-service (HTTPS)
Render Postgres (multi-DB) + Redis
```

| In 8a | Out of 8a (documented, not abandoned) |
|---|---|
| Web, tennis-data, match, Postgres, Redis | Eureka, config-server |
| Absolute service URLs (no discovery) | api-gateway public `/api/v1` mesh |
| Catalogue + ingest HTTP paths | Kafka consumers (replay auto, analytics, notifications) |
| | Elasticsearch / analytics-service |
| | MinIO/R2 replay storage pipeline |

**Why:** Vercel cannot join Render’s private network. Each BFF upstream must be a public HTTPS service (or one public gateway — 8b). Eureka on ephemeral Render hosts is busywork; env URLs are the FAANG-honest move.

## Vercel

1. Import the GitHub repo in Vercel.
2. Framework: Next.js. **Root Directory:** leave repo root (pnpm workspace).
3. Install: `pnpm install --frozen-lockfile`
4. Build: `pnpm --filter @tennisly/web build`
5. Output: Next default (no `standalone` required).
6. Node: **22** (see root `packageManager` / `engines`).
7. Copy env from `apps/web/.env.production.example`.

`vercel.json` at repo root pins install/build for monorepo.

### Clerk

- Use **live** keys only on the production Vercel project (or a dedicated staging project with test keys).
- Dashboard → allowed origins / redirect URLs = your `*.vercel.app` (and custom domain later).
- CSP in `next.config.ts` allows Clerk + optional `CSP_CONNECT_SRC_EXTRA` for Render WS later.

## Render

Use Blueprint: `render.yaml` (repo root).

### Services

| Service | Health | Notes |
|---|---|---|
| `tennis-data-service` | `GET /actuator/health` | Requires `TENNIS_BALLDONTLIE_API_KEY` + `TENNIS_LIVETENNIS_API_KEY` (fail-fast) |
| `match-service` | `GET /actuator/health` | `TENNIS_DATA_SERVICE_URI` = public tennis-data URL; Redis for live snapshots |

### Data stores

- **Postgres** — one instance; create DBs `tennisly_tennis_data`, `tennisly_matches` (same as local init).
- **Redis** — rate-limit/live snapshot cache.

### Eureka / Kafka (8a)

```bash
EUREKA_CLIENT_ENABLED=false
MANAGEMENT_HEALTH_KAFKA_ENABLED=false
```

Producers log and continue if the broker is absent; **do not claim** event-driven features work until 8b (managed Kafka or Redpanda).

### Build

Dockerfiles are **multi-stage** (Maven → JRE). Render `dockerfilePath` + repo-root context.

## Secrets checklist

| Where | Secrets |
|---|---|
| Vercel | Clerk publishable + secret; `MATCH_SERVICE_URL`; `TENNIS_DATA_SERVICE_URL`; optional user/replay/analytics URLs |
| Render tennis-data | BallDontLie + LiveTennis keys; `POSTGRES_*`; `REDIS_*` |
| Render match | `POSTGRES_*`; `REDIS_*`; `TENNIS_DATA_SERVICE_URI`; `MATCH_SERVICE_WS_ALLOWED_ORIGINS` (include Vercel origin) |

Never commit live keys. Rotate after any paste into chat/logs.

## Phase 8b / 8c (next)

1. **8b:** user-service + auth-service (Clerk webhooks) + optional api-gateway; managed Kafka; notification worker.
2. **8c:** analytics + Elasticsearch; R2 for replay objects; custom domains + prod CORS allowlist.

## Local vs cloud

| Local | Cloud |
|---|---|
| `make up` + Eureka + Kafka + ES | No Eureka; Kafka deferred; ES deferred |
| Host ports from `.run/ports.env` | Render assigns `PORT` — bind `SERVER_PORT=$PORT` |
| Web → localhost services | Web → `https://*.onrender.com` |

## After Phase 8 — performance achievement (later)

Ultra-low-latency work returns as a **measured** Week 31+ achievement (cache hierarchy, colo, pool tuning, load proof).  
**Honesty bound:** browser→Vercel→Render RTT cannot be “everything under 1ms” on the public internet. Sub-1ms targets apply to **in-process / same-AZ Redis / JVM hot paths**, with published p50/p99 numbers — not marketing fiction.
