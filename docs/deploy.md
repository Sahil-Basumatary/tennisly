# Phase 8 deploy — Vercel (web) + Render (API)

Replaces the plan’s EKS/Helm path with a portfolio-honest cloud cut:
**Next.js on Vercel** (`tennisly.tv`), **Spring services on Render**.

## Topology (Phase 8a — first public demo)

```
Browser
  → Vercel @tennisly/web  (https://tennisly.tv)
       → Render api-gateway   (https://api-gateway-….onrender.com → api.tennisly.tv)
            → tennis-data-service  (token-locked)
            → match-service        (token-locked)
Neon Postgres (multi-DB) + Render Redis
```

| In 8a | Out of 8a (documented, not abandoned) |
|---|---|
| Web, api-gateway, tennis-data, match, Neon, Redis | Eureka, config-server |
| Shared-secret backends on free web plans | Paid Render private services |
| Catalogue GETs through gateway (no JWT) | Metered `/api/v1` + user-service API keys |
| Match ingest via tennis-data (same secret) | Kafka consumers, analytics, replay storage |

**Why gateway + shared secret (not private services):** Render private services start at ~$7/mo each. Free web services stay publicly routable, so `GATEWAY_INTERNAL_TOKEN` (`X-Gateway-Token`) is the lock. The gateway stamps every proxied request; match-service presents the same header when calling tennis-data for ingest. Blank token = filter off (local `make up` unchanged).

**Why `/api/v1` waits for 8b:** API-key validation calls user-service. Without it, every public API key request would 401. Catalogue for the web app uses service-native paths (`/api/tennis/**`, `/api/matches/**`) through the gateway with GET permitAll.

## Domain — `tennisly.tv` (Namecheap → Vercel)

You already own the domain. Replace the Namecheap parking records before go-live.

### 1. Add the domain in Vercel

Project → Settings → Domains → add `tennisly.tv` and `www.tennisly.tv`. Vercel shows the exact records; they are usually:

| Type | Host | Value |
|---|---|---|
| A | `@` | `76.76.21.21` |
| CNAME | `www` | `cname.vercel-dns.com` |

### 2. Namecheap Advanced DNS

On [Advanced DNS for tennisly.tv](https://ap.www.namecheap.com/Domains/DomainControlPanel/tennisly.tv/advancedns):

1. **Delete** the parking `CNAME` (`www` → `parkingpage.namecheap.com`).
2. **Delete** the `URL Redirect Record` (`@` → `http://www.tennisly.tv/`).
3. **Add** the A + CNAME rows from the table above (TTL Automatic / 30 min is fine).

Propagation is usually minutes, sometimes up to an hour. `dig tennisly.tv +short` should show `76.76.21.21`.

### 3. Optional API subdomain

After the gateway is live on Render:

| Type | Host | Value |
|---|---|---|
| CNAME | `api` | `api-gateway-xxxx.onrender.com` |

Then set Vercel `MATCH_SERVICE_URL` / `TENNIS_DATA_SERVICE_URL` to `https://api.tennisly.tv`.

### 4. Clerk dashboard (live keys)

Allowed origins / redirect URLs:

- `https://tennisly.tv`
- `https://www.tennisly.tv`
- (keep `*.vercel.app` for preview deploys if you use them)

## Vercel

1. Import the GitHub repo.
2. Framework: Next.js.
3. **Root Directory: `apps/web`** (required — Vercel detects `next` from that package.json; the repo root only has workspace tooling).
4. Config lives in **`apps/web/vercel.json` only** (do not put `"framework": "nextjs"` in a repo-root `vercel.json` — that makes Vercel probe the root `package.json` and fail with “No Next.js version detected”).
5. Install runs from the monorepo root; build is `pnpm build` inside `apps/web`.
6. Node **22**.
7. Env from `apps/web/.env.production.example` — both upstream URLs point at **api-gateway**, not the backends.

Do **not** leave Root Directory empty.

## Render

Blueprint: `render.yaml`.

| Service | Role | Notes |
|---|---|---|
| `api-gateway` | Only advertised public API | Stamps `X-Gateway-Token`; Redis rate limits; Clerk JWT for non-catalogue `/api/**` |
| `tennis-data-service` | Catalogue + provider sync | Rejects requests without the token (except `/actuator/health`) |
| `match-service` | Matches + ingest | Same token; calls tennis-data with the token |
| `tennisly-redis` | Free Key Value | Rate limit + live snapshots — not a system of record |

Generate the shared secret once:

```bash
openssl rand -hex 32
```

Paste the **same** value into `GATEWAY_INTERNAL_TOKEN` on all three services when Render prompts (`sync: false`).

After first deploy, set:

- Gateway `TENNIS_DATA_SERVICE_URI` / `MATCH_SERVICE_URI` / `MATCH_SERVICE_WS_URI` = the backend `*.onrender.com` URLs
- Match `TENNIS_DATA_SERVICE_URI` = tennis-data URL
- Gateway `CLERK_JWKS_URI` + `CLERK_ISSUER_URI` from your Clerk instance

### Data stores

**Postgres — Neon** (not Render free DB — that expires after 30 days).

```bash
psql "postgresql://USER:PASSWORD@HOST/neondb?sslmode=require" \
  -f infrastructure/neon/init-databases.sql
```

| Env var | Value | Why |
|---|---|---|
| `POSTGRES_URL_PARAMS` | `?sslmode=require` | Neon requires TLS |
| `POSTGRES_POOL_MAX` | `5` | Serverless connection caps |
| `POSTGRES_HOST` | Neon **pooler** host | Survives scale-to-zero |

### Eureka / config-server / Kafka (8a)

```bash
EUREKA_CLIENT_ENABLED=false
CONFIG_SERVER_ENABLED=false
GATEWAY_DISCOVERY_LOCATOR_ENABLED=false
MANAGEMENT_HEALTH_KAFKA_ENABLED=false
TENNISLY_KAFKA_ENABLED=false
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
TENNIS_DATA_SEED_ON_STARTUP=false
```

Without Kafka disabled, the producer AdminClient retries `localhost:9092` and startup catalogue seed (BallDontLie at 5 req/min) can hang a free-tier deploy for a long time. Seed after health is green with `SEED=true make verify-deploy`.
### Build

```bash
make render-images
```

Multi-stage Dockerfiles from repo root; `.dockerignore` is deny-by-default so context stays ~100KB.

## Verify the deploy

Prefer the gateway (what users and Vercel hit):

```bash
export GATEWAY_URL=https://api-gateway-xxxx.onrender.com
make verify-deploy
```

Catalogue checks go through the gateway. `SEED=true` still hits tennis-data **directly** with `GATEWAY_INTERNAL_TOKEN` (sync stays JWT-protected on the gateway):

```bash
export TENNIS_DATA_URL=https://tennis-data-service-xxxx.onrender.com
export GATEWAY_INTERNAL_TOKEN=...   # same as Render
SEED=true make verify-deploy
```

## Secrets checklist

| Where | Secrets |
|---|---|
| Vercel | Clerk live keys; `MATCH_SERVICE_URL` + `TENNIS_DATA_SERVICE_URL` = gateway; optional `CSP_CONNECT_SRC_EXTRA` |
| Neon | Split into `POSTGRES_HOST/USER/PASSWORD` on tennis-data + match |
| Render (all three) | Same `GATEWAY_INTERNAL_TOKEN` |
| Render tennis-data | BallDontLie + LiveTennis keys; `POSTGRES_*` |
| Render match | `POSTGRES_*`; `TENNIS_DATA_SERVICE_URI` |
| Render gateway | `TENNIS_DATA_SERVICE_URI`; `MATCH_SERVICE_URI`; `MATCH_SERVICE_WS_URI`; `CLERK_*`; `CORS_ALLOWED_ORIGINS` |

Never commit live keys. Rotate after any paste into chat/logs.

## Phase 8b / 8c

**8b sequence** (one slice at a time; Kafka stays off on free Render):

1. **This cut:** cloud-boot auth-service + user-service (Docker, Neon DBs, Kafka disabled, shared token).
2. Wire gateway `AUTH_SERVICE_URI` / `USER_SERVICE_URI` / `USER_SERVICE_ROUTE_URI`; Clerk → `https://api-gateway-….onrender.com/api/auth/webhooks/clerk`.
3. Auth HTTP-projects Clerk users/orgs to user-service (`USER_SERVICE_URI` on auth). Kafka stays off on free Render.
4. Enable `/api/v1` against a live user-service (API keys).
5. **notification-service:** Docker + Neon `tennisly_notifications`, Kafka off, HTTP ingest from user-service/match-service (`NOTIFICATION_SERVICE_URI` on those backends). Gateway `NOTIFICATION_SERVICE_URI`. Vercel `NOTIFICATION_SERVICE_URL` = gateway. Email/push stay `logging` until Resend/FCM. Live Test delivery is deferred (free-tier sleep + Clerk JWT 401).

**8c sequence** (one slice at a time; Kafka stays off on free Render):

1. **This cut:** Docker + shared token + Kafka/Eureka off for analytics-service. Neon `tennisly_analytics` (SQL only; do not create a Render service yet). Do **not** run Elasticsearch on Render free (512MB will OOM). HTTP ingest and Elastic Cloud come next.
2. HTTP ingest from match-service (same dual-write as notification; Kafka stays off).
3. Elastic Cloud (or equivalent) URL on analytics-service; gateway `ANALYTICS_SERVICE_URI`; Vercel `ANALYTICS_SERVICE_URL` = **gateway**. Reindex via `/internal/analytics/reindex`.
4. R2 for replay objects.
5. Paid always-on only if you want a live demo without sleep.

### 8b secrets (auth + user)

| Where | Secrets |
|---|---|
| Neon | Extra DBs `tennisly_auth`, `tennisly_users` via `infrastructure/neon/init-databases.sql` |
| Render auth | Same `GATEWAY_INTERNAL_TOKEN`; `CLERK_WEBHOOK_SECRET`; `USER_SERVICE_URI`; `POSTGRES_*` / `POSTGRES_DB_AUTH` |
| Render user | Same token; `WEBHOOK_ENCRYPTION_KEY`; `POSTGRES_DB_USERS`; `NOTIFICATION_SERVICE_URI` = notification-service HTTPS URL |
| Render match | Same token; `NOTIFICATION_SERVICE_URI` = notification-service HTTPS URL |
| Render gateway | `AUTH_SERVICE_URI`; `USER_SERVICE_URI` + `USER_SERVICE_ROUTE_URI` (same user URL) |

### 8b secrets (notification)

| Where | Secrets |
|---|---|
| Neon | Extra DB `tennisly_notifications` (same init SQL) |
| Render notification | Same `GATEWAY_INTERNAL_TOKEN`; `NOTIFICATION_USER_SERVICE_URI` = user-service HTTPS URL (not gateway — `/internal/**` is not routed); `POSTGRES_DB_NOTIFICATIONS` |
| Render gateway | `NOTIFICATION_SERVICE_URI` = notification-service HTTPS URL |
| Vercel | `NOTIFICATION_SERVICE_URL` = **gateway** URL (same as `USER_SERVICE_URL`) |

Two more free JVMs will sleep like tennis-data/match. Do not treat a cold 502 as a code regression.

### 8c secrets (analytics)

| Where | Secrets |
|---|---|
| Neon | Extra DB `tennisly_analytics` (same init SQL) |
| Render analytics | Same token; `ELASTICSEARCH_URI` (Elastic Cloud, not a Render ES box); `ANALYTICS_MATCH_SERVICE_URI` = match-service HTTPS (not gateway — `/internal/**` is not routed); `POSTGRES_DB_ANALYTICS` |
| Render gateway | `ANALYTICS_SERVICE_URI` = analytics-service HTTPS URL |
| Vercel | `ANALYTICS_SERVICE_URL` = **gateway** URL (same as `USER_SERVICE_URL`) |

Do not create the Render analytics service until `ELASTICSEARCH_URI` exists. Boot without ES fails.

## Local vs cloud

| Local | Cloud |
|---|---|
| `make up` + Eureka + Kafka | No Eureka; Kafka deferred |
| Token blank → filter off | Token required on backends |
| Web → localhost services | Web → gateway HTTPS |

## After Phase 8 — performance achievement (later)

Ultra-low-latency work returns as a **measured** Week 31+ achievement.  
**Honesty bound:** browser→Vercel→Render RTT cannot be “everything under 1ms” on the public internet. Sub-1ms targets apply to in-process / same-AZ Redis / JVM hot paths with published p50/p99 — not marketing fiction.
