# Tennisly

Interactive tennis match visualization and data analytics platform.

## About

A tool for visualizing tennis matches with real-time tracking, replays, and statistics.

## Data provenance

Tennisly does not invent match results, rankings, or player identity.

| Surface | Source | Notes |
|---------|--------|-------|
| Players, rankings, tournaments | [BallDontLie](https://www.balldontlie.io/) Free (`TENNIS_BALLDONTLIE_API_KEY`) | Synced into `tennis-data-service` |
| Live / upcoming / completed matches + point tape | [Live Tennis API](https://livetennisapi.com/) BASIC (`TENNIS_LIVETENNIS_API_KEY`) | Normalized in `tennis-data-service`, ingested by `match-service` |
| Point ledger (server, winner, sequence, score snapshot) | Derived from Live Tennis score rows | Outcome is `UNKNOWN`; rally length is null until synthesis |
| Ball flight / shot frames | Physics synthesizer in `replay-service` | Built from the real ledger + surface shot-distribution priors |

Both API keys are required for `tennis-data-service` to start. Without them the process fails fast instead of falling back to fabricated data. The web app shows empty states when upstreams are down and never serves a silent mock rally.

### Trajectory synthesis

Shot-level tracking (Hawkeye / Tennis Data Innovations) is separately licensed. Tennisly therefore synthesizes ball trajectories from:

1. The real point-by-point ledger (who served, who won, score after the point)
2. Surface shot-distribution priors (`V2__seed_shot_distributions.sql` in replay-service) — model parameters, not observed shots

The court UI labels this with a **Synthesized trajectory** badge. Match stats only show tape-provable metrics (points won, service points, breaks) — not aces / winners / unforced errors invented from thin air.

### Analytics

Phase 5 indexes that same tape into Elasticsearch (`tennisly-match-analytics` / `tennisly-player-match` aliases) via `analytics-service`. Public reads power `/analytics` in the web app; saved views require Clerk. Operations, rebuild, and API notes: [docs/analytics.md](docs/analytics.md).

## Local configuration

See `apps/web/.env.local.example` and service `application.yml` files. Required secrets:

```bash
TENNIS_BALLDONTLIE_API_KEY=...
TENNIS_LIVETENNIS_API_KEY=...
```

Optional BallDontLie knobs (useful after downgrading to Free): `TENNIS_BALLDONTLIE_PER_PAGE` (default `100`), `TENNIS_BALLDONTLIE_REQUESTS_PER_MINUTE` (default `5`), `TENNIS_BALLDONTLIE_MAX_PAGES`.

### Local ports

Tennisly uses its own host port block (e.g. postgres `15432`, redis `16379`, web `13000`) so it can run beside other Docker stacks. `make up` runs `scripts/allocate-ports.sh`: if a preferred port is busy it walks upward to the next free port and writes `.run/ports.env`. Inspect with `make ports-print`.

Optional ingest toggles on match-service: `MATCH_INGEST_ENABLED`, `MATCH_INGEST_LIVE_DELAY_MS`, `MATCH_INGEST_COMPLETED_DELAY_MS`.

## Status

Work in progress — Phase 8 cloud cut targets **Vercel** (web) + **Render** (API). See [docs/deploy.md](docs/deploy.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## Deploy (Phase 8a)

| Layer | Host | Config |
|---|---|---|
| Next.js web | Vercel | `vercel.json`, `apps/web/.env.production.example` |
| tennis-data + match | Render | `render.yaml`, `infrastructure/render/.env.example` |
| Postgres + Redis | Render | Blueprint-managed |

Day-1 demo is catalogue HTTP (no Eureka, Kafka deferred). Expand per `docs/deploy.md` 8b/8c.

## Contact

- Email: sahil@sahilbasumatary.dev
- LinkedIn: [Sahil Basumatary](https://www.linkedin.com/in/sahil-basumatary/)

## License

MIT
