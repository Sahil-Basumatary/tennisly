# Testing strategy

Phase 7 quality bar for Tennisly. Goal over Weeks 29–31: raise confidence without pretending we already have 80% everywhere.

## Layers

| Layer | Tooling | Where it runs |
|---|---|---|
| Unit | JUnit 5 + Mockito | `mvn test` / CI backend job |
| Coverage | Jacoco reports on every module; **≥70% line on webhook + notification packages in `tennisly-common`** | CI `jacoco:report` + `verify -Djacoco.skip.check=false` for common |
| Integration | Testcontainers Postgres (`PublicWebhookApiIT`, `WebhookDeliveryWorkerIT`) | Local + CI (Docker required); skips cleanly if Docker is unavailable |
| Frontend unit | Turbo / package scripts | CI frontend job (lint, type-check, test, build) |
| E2E smoke | Playwright (`apps/web/e2e`) | Local `make e2e`; optional CI when `RUN_PLAYWRIGHT=true` + Clerk secrets |
| Load smoke | k6 | Local / staging: `k6 run tests/load/public-api-smoke.js` |
| Pact / mutation | Planned | Not gated in CI yet |

## Local commands

```bash
# Core JVM suites used in day-to-day make
make test

# Testcontainers ITs (Docker Desktop running)
make test-it

# Playwright smoke (Clerk keys in apps/web/.env.local)
make e2e

# Broader suite (includes gateway + common + users)
./mvnw -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service,services/match-service,services/tennis-data-service,services/analytics-service -am test

# Coverage HTML: services/<svc>/target/site/jacoco/index.html
./mvnw test jacoco:report -pl services/tennisly-common -am

# Enforce common floor locally
./mvnw -pl services/tennisly-common verify -Djacoco.skip.check=false
```

## Integration tests (Testcontainers)

- `PublicWebhookApiIT` (user-service) — Postgres 16 + Flyway + MockMvc for public webhook create/list, SSRF loopback reject, tenant headers.
- `WebhookDeliveryWorkerIT` (notification-service) — Postgres outbox + real HTTP receiver: enqueue → SUCCESS + HMAC verify, FAILED retry scheduling. Kafka listeners disabled under `it` profile.
- Profile: `it` with Docker Engine 29+ needing `docker-java.properties` (`api.version=1.44`).
- `@Testcontainers(disabledWithoutDocker = true)` so machines without Docker stay green (tests skipped, not failed).
- Run: `make test-it`

## Playwright smoke

- Specs in `apps/web/e2e/smoke.spec.ts`: `/api/health` + security headers, home/about brand, unauthenticated redirects for `/dashboard`, `/settings/notifications`, `/admin`.
- Uses `@clerk/testing` tokens + `next start` on port `3110` (set `PLAYWRIGHT_BASE_URL` to point at an already-running app).
- First-time browser install: `pnpm --filter @tennisly/web exec playwright install chromium`.
- Run: `make e2e` (builds web, then Playwright).

## Coverage policy (honest)

- Parent property `jacoco.skip.check=true` so greenfield services do not fail CI overnight.
- Shared security-sensitive packages in `tennisly-common` (`webhook`, `notification`) must stay ≥ 70% line coverage — ratchet this up and expand package scope as Phase 7 continues.
- Portfolio target remains **>80%** on business-critical packages (auth, match scoring, webhook crypto). Do not claim 80% until Jacoco shows it.

## Next slices

1. Pact contracts for `/api/v1/**` between gateway consumers and tennis-data/match producers.
2. Raise Jacoco floors module-by-module (gateway filters → user-service security → notification worker).
3. OWASP/CORS/actuator prod lockdown.
4. Authenticated Playwright flows (Clerk test user storageState).
