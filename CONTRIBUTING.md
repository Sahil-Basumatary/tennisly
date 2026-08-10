# Contributing to Tennisly

## Ground rules

- Prefer small commits with conventional messages (`feat:`, `fix:`, `perf:`, `docs:`, `chore:`).
- Do not commit secrets (`.env`, Clerk live keys, API keys). Use examples only.
- Security and tenancy beats micro-optimizations.

## Local setup

1. JDK **21**, Node **22**, pnpm **9.15**, Docker Desktop.
2. Copy `apps/web/.env.local.example` → `apps/web/.env.local` (Clerk test keys).
3. Set `TENNIS_BALLDONTLIE_API_KEY` and `TENNIS_LIVETENNIS_API_KEY` for tennis-data.
4. `make up` (or `make infra-up` + run services) — see root `README.md`.
5. `pnpm install && pnpm --filter @tennisly/web dev`

## Quality gates

```bash
make test
make test-pact
make e2e
./mvnw -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service verify -Djacoco.skip.check=false
```

Optional: `make zap-api` (disposable API key + gateway or stub).

## Cloud deploy

See [docs/deploy.md](docs/deploy.md) — **Vercel** (web) + **Render** (JVM + Postgres + Redis).

## PR expectations

- Describe *why*, not a file dump.
- Include a short test plan checklist.
- Keep diffs focused; no drive-by refactors.
