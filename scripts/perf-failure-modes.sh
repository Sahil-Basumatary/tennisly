#!/usr/bin/env bash
# Failure-mode notes for warm Render / local stack. Do not fail open.
#
#   ./scripts/perf-failure-modes.sh
set -euo pipefail

cat <<'EOF'
Failure-mode experiments (manual, warm services only)

1. Redis latency / flush
   - Local: docker pause tennisly-redis (or equivalent compose name)
   - Expect: catalogue still serves from DB; live snapshots miss; API-key cache misses
   - Must not: gateway 500-open rate limits (fail closed when RATE_LIMIT_FAIL_OPEN=false)

2. Neon saturation
   - Lower POSTGRES_POOL_MAX=2 and rerun SCENARIO=burst ./scripts/k6-load.sh
   - Expect: p99 climbs, errors stay <0.1% until pool wait exceeds timeouts

3. Downstream timeout
   - Point analytics/replay URIs at a black hole; record a point
   - Expect: HTTP 200 on recordPoint; outbox retries; match score persisted

4. Rate limit
   - Hit /api/v1 with a FREE key above 30/min
   - Expect: 429, never 200 with stripped auth

Cold starts belong in a separate folder from warm SLO JSON.
EOF
