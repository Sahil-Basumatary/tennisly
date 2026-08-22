# Testing strategy

Phase 7 quality bar for Tennisly. Goal over Weeks 29–31: raise confidence without pretending we already have 80% everywhere.

## Layers

| Layer | Tooling | Where it runs |
|---|---|---|
| Unit | JUnit 5 + Mockito | `mvn test` / CI backend job |
| Coverage | Jacoco reports on every module; **≥80%** common webhook+notification; **≥70%** gateway filters/ratelimit; **≥70%** user-service `security`; **≥70%** notification worker+enqueue+security | CI `verify -Djacoco.skip.check=false` for those modules |
| Integration | Testcontainers Postgres (`PublicWebhookApiIT`, `WebhookDeliveryWorkerIT`) | Local + CI (Docker required); skips cleanly if Docker is unavailable |
| Frontend unit | Turbo / package scripts | CI frontend job (lint, type-check, test, build) |
| E2E smoke | Playwright (`apps/web/e2e`) | Local `make e2e`; optional CI when `RUN_PLAYWRIGHT=true` + Clerk secrets |
| Load | k6 | Public reads: `./scripts/k6-load.sh`; local Postgres writes: `make load-durable`; STOMP fanout: `make load-websocket`; `/api/v1`: `tests/load/public-api-v1.js` |
| Security scan | OWASP ZAP api-scan | Local `make zap-api`; optional CI when `RUN_ZAP=true` + staging secrets |
| Contract | Pact JVM (`api-gateway` ↔ tennis-data players, match-service matches, user-service webhooks) | `make test-pact`; committed JSON under `tests/pacts/` |
| Pact / mutation | Mutation still planned | Pact gated via module tests in CI |

## Local commands

```bash
# Core JVM suites used in day-to-day make
make test

# Testcontainers ITs (Docker Desktop running)
make test-it

# Playwright smoke (Clerk keys in apps/web/.env.local)
make e2e

# OWASP ZAP against a live gateway (disposable API_KEY; Docker required)
# API_KEY=tly_live_... TARGET_URL=http://host.docker.internal:8080 make zap-api
# Local triage without the Spring stack (header-faithful stub on :18080):
#   API_KEY=tly_live_... make zap-stub   # separate terminal
#   API_KEY=tly_live_... TARGET_URL=http://host.docker.internal:18080 make zap-api

# Pact: regenerate consumer contracts then verify providers
make test-pact

# Broader suite (includes gateway + common + users)
./mvnw -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service,services/match-service,services/tennis-data-service,services/analytics-service -am test

# Local Postgres commit → STOMP client delivery, p99 < 50 ms
make load-websocket

# All clients on one topic; this is not a 100k claim
WS_MODE=hot WS_CLIENTS=500 make load-websocket

# Coverage HTML: services/<svc>/target/site/jacoco/index.html
./mvnw test jacoco:report -pl services/tennisly-common -am

# Enforce common + gateway + user-service + notification worker floors locally
./mvnw -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service verify -Djacoco.skip.check=false
```

## Integration tests (Testcontainers)

- `PublicWebhookApiIT` (user-service) — Postgres 16 + Flyway + MockMvc for public webhook create/list, SSRF loopback reject, tenant headers.
- `WebhookDeliveryWorkerIT` (notification-service) — Postgres outbox + real HTTP receiver: enqueue → SUCCESS + HMAC verify, FAILED retry scheduling. Kafka listeners disabled under `it` profile.
- Profile: `it` with Docker Engine 29+ needing `docker-java.properties` (`api.version=1.44`).
- `@Testcontainers(disabledWithoutDocker = true)` so machines without Docker stay green (tests skipped, not failed).
- Run: `make test-it`

## Playwright smoke

- Specs in `apps/web/e2e/smoke.spec.ts`: `/api/health` + security headers, home/about brand, unauthenticated redirects for `/dashboard`, `/settings/notifications`, `/admin`.
- Authenticated specs in `apps/web/e2e/authenticated.spec.ts` (dashboard + settings) when `E2E_CLERK_USER_EMAIL` is set — project `setup` signs in via `@clerk/testing` ticket and writes `playwright/.clerk/user.json` (gitignored).
- Prefer a `+clerk_test` Clerk user so Clerk suppresses email delivery.
- Always runs against **`next start`** (production build). Turbopack/`next dev` is intentionally unsupported — it panics and causes Clerk proxy loops.
- `@clerk/testing` tokens required; Clerk keys from `apps/web/.env.local`.
- Port `3110` by default. Override with `PLAYWRIGHT_BASE_URL` / `PLAYWRIGHT_PORT`.
- First-time browser install: `pnpm --filter @tennisly/web exec playwright install chromium`.
- Run: `make e2e` or `pnpm --filter @tennisly/web test:e2e` (both build first). Use `test:e2e:only` only when `.next` already exists.

## Pact contracts

- Consumer module: `services/contract-tests` writes:
  - `tests/pacts/api-gateway-tennis-data-service.json` (`ApiGatewayPlayersPactTest`)
  - `tests/pacts/api-gateway-match-service.json` (`ApiGatewayMatchesPactTest` — list + by id)
  - `tests/pacts/api-gateway-user-service.json` (`ApiGatewayWebhooksPactTest` — list)
- Providers (standalone MockMvc, no DB):
  - tennis-data `PlayerControllerProviderPactTest`
  - match-service `MatchControllerProviderPactTest`
  - user-service `PublicWebhookControllerProviderPactTest` (sets `RequestContext` org for `X-Org-Id`)
- Contract paths are **service** paths after gateway rewrite (`/api/tennis/...`, `/api/matches`, `/api/users/public/webhooks`).
- After changing a consumer DSL, re-run `make test-pact` and **commit** the updated pact JSON.

## Coverage policy (honest)

- Parent property `jacoco.skip.check=true` so greenfield services do not fail CI overnight.
- Shared security-sensitive packages in `tennisly-common` (`webhook`, `notification`) must stay ≥ **80%** line coverage.
- Gateway auth/rate-limit packages (`filter`, `ratelimit`) must stay ≥ **70%** line coverage.
- User-service crypto/SSRF/API-key helpers (`security/**`) must stay ≥ **70%** line coverage.
- Notification worker packages (`WebhookDeliveryWorker`, `BackoffCalculator`, `EnqueueService`, `security/**`) must stay ≥ **70%** line coverage.
- Portfolio target remains **>80%** on business-critical packages. Do not claim 80% repo-wide until Jacoco shows it.

## Next slices

1. Re-run ZAP against a real staging gateway (`RUN_ZAP=true` + secrets) — stub triage already green.
2. Optional Pact Broker when multi-repo consumers appear.
3. Optional `context/**` Jacoco after `TenantInterceptor` tests.
4. Set repo secret `E2E_CLERK_USER_EMAIL` (prefer `+clerk_test`) when enabling authenticated Playwright in CI.