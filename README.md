# Tennisly

[tennisly.tv](https://tennisly.tv) is a live tennis platform for scores, rankings, match replays, and point-level analytics.

## What it includes

- Live, upcoming, and completed matches with major tournaments shown first
- ATP and WTA rankings, player profiles, schedules, and results
- Interactive 2D court replays with an optional 3D view
- Match and player analytics built from point history
- Saved analytics views, account settings, API keys, and webhooks


## Stack

- Next.js 16, React 19, TypeScript, Tailwind CSS, Zustand, and Babylon.js
- Java 21 and Spring Boot services for tennis data, matches, replays, analytics, users, and notifications
- PostgreSQL for match, account, and delivery state
- Redis for caching and live match updates
- Kafka for events between services
- Elasticsearch for match and player analytics
- MinIO locally and S3-compatible storage for replay files
- Clerk for authentication
- Vitest, Playwright, JUnit, Testcontainers, Pact, and k6 for verification

## Architecture

```text
Next.js web app
  -> API gateway
     -> tennis-data service
     -> match service
     -> replay service
     -> analytics service
     -> user and notification services

BallDontLie + Live Tennis API -> tennis data -> matches
matches -> Kafka -> replays, analytics, and notifications
live match updates -> Redis and WebSocket -> web app
```

## Run locally

Requirements:

- Java 21
- Node.js 22
- pnpm 9
- Docker
- GNU Make

```bash
git clone https://github.com/Sahil-Basumatary/tennisly.git
cd tennisly

corepack enable
pnpm install

cp infrastructure/docker/.env.example infrastructure/docker/.env
cp apps/web/.env.local.example apps/web/.env.local
```

Set `TENNIS_BALLDONTLIE_API_KEY` and `TENNIS_LIVETENNIS_API_KEY` in `infrastructure/docker/.env`. Set the Clerk keys in `apps/web/.env.local` for signed-in features.

Start the platform:

```bash
make up
make ports-print
```

The web app uses [http://localhost:13000](http://localhost:13000) by default. Run `make ports-print` if that port was already in use.

Stop everything with:

```bash
make down
```

## Checks

```bash
make test
pnpm lint
pnpm --filter @tennisly/web type-check
pnpm --filter @tennisly/web test
make e2e
```

Deployment, analytics, and contribution notes live in [`docs/`](docs/) and [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Contact

- Email: [sahil@sahilbasumatary.dev](mailto:sahil@sahilbasumatary.dev)
- Website: [sahilbzy.com](https://sahilbzy.com)
- GitHub: [Sahil-Basumatary](https://github.com/Sahil-Basumatary)
- LinkedIn: [Sahil Basumatary](https://www.linkedin.com/in/sahil-basumatary/)

## License

Tennisly is open source under the [MIT License](LICENSE).
