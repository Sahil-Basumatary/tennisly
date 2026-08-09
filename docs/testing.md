# Testing strategy

Phase 7 quality bar for Tennisly. Goal over Weeks 29–31: raise confidence without pretending we already have 80% everywhere.

## Layers

| Layer | Tooling | Where it runs |
|---|---|---|
| Unit | JUnit 5 + Mockito | `mvn test` / CI backend job |
| Coverage | Jacoco reports on every module; **≥70% line on webhook + notification packages in `tennisly-common`** | CI `jacoco:report` + `verify -Djacoco.skip.check=false` for common |
| Frontend | Turbo / package scripts | CI frontend job (lint, type-check, test, build) |
| Load smoke | k6 | Local / staging: `k6 run tests/load/public-api-smoke.js` |
| E2E / Pact / mutation | Planned | Not gated in CI yet |

## Local commands

```bash
# Core JVM suites used in day-to-day make
make test

# Broader suite (includes gateway + common + users)
./mvnw -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service,services/match-service,services/tennis-data-service,services/analytics-service -am test

# Coverage HTML: services/<svc>/target/site/jacoco/index.html
./mvnw test jacoco:report -pl services/tennisly-common -am

# Enforce common floor locally
./mvnw -pl services/tennisly-common verify -Djacoco.skip.check=false
```

## Coverage policy (honest)

- Parent property `jacoco.skip.check=true` so greenfield services do not fail CI overnight.
- Shared security-sensitive packages in `tennisly-common` (`webhook`, `notification`) must stay ≥ 70% line coverage — ratchet this up and expand package scope as Phase 7 continues.
- Portfolio target remains **>80%** on business-critical packages (auth, match scoring, webhook crypto). Do not claim 80% until Jacoco shows it.

## Next slices

1. Playwright smoke for `/`, login-gated `/dashboard`, admin health.
2. Testcontainers integration for user-service webhooks + notification delivery worker.
3. Pact contracts for `/api/v1/**` between gateway consumers and tennis-data/match producers.
4. Raise Jacoco floors module-by-module (gateway filters → user-service security → notification worker).
