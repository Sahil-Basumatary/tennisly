import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * Public catalogue GETs through the gateway (permitAll, no API key).
 *
 *   BASE_URL=https://api-gateway-….onrender.com SCENARIO=smoke ./scripts/k6-load.sh
 *
 * Warm-up is a separate k6 stage so cold samples never enter p50/p95/p99.
 */

const playersTrend = new Trend('catalogue_players_ms', true);
const rankingsTrend = new Trend('catalogue_rankings_ms', true);
const matchesTrend = new Trend('catalogue_matches_ms', true);
const non200 = new Rate('catalogue_non_200');
const successes = new Counter('catalogue_http_200');

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const SCENARIO = __ENV.SCENARIO || 'smoke';

const PATHS = [
  { name: 'players', path: '/api/tennis/players?gender=MALE', trend: playersTrend },
  {
    name: 'rankings',
    path: '/api/tennis/rankings?gender=MALE&type=SINGLES',
    trend: rankingsTrend,
  },
  { name: 'matches', path: '/api/matches?status=IN_PROGRESS', trend: matchesTrend },
];

const SCENARIOS = {
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
    startTime: '10s',
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '20s', target: 8 },
      { duration: '40s', target: 8 },
      { duration: '10s', target: 0 },
    ],
    startTime: '15s',
  },
  burst: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '5s', target: 20 },
      { duration: '15s', target: 20 },
      { duration: '5s', target: 0 },
    ],
    startTime: '10s',
  },
  soak: {
    executor: 'constant-vus',
    vus: 4,
    duration: '30m',
    startTime: '20s',
  },
};

const WARMUP = {
  smoke: { duration: '10s', target: 1 },
  load: { duration: '15s', target: 2 },
  burst: { duration: '10s', target: 2 },
  soak: { duration: '20s', target: 2 },
};

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: 1,
      duration: WARMUP[SCENARIO].duration,
      exec: 'warmup',
    },
    measured: Object.assign({ exec: 'measured' }, SCENARIOS[SCENARIO] || SCENARIOS.smoke),
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],
    catalogue_non_200: ['rate<0.001'],
    http_req_duration: ['p(50)<100', 'p(95)<250', 'p(99)<500'],
    catalogue_players_ms: ['p(95)<250', 'p(99)<500'],
    catalogue_rankings_ms: ['p(95)<250', 'p(99)<500'],
    catalogue_matches_ms: ['p(95)<250', 'p(99)<500'],
  },
};

function hitCatalogue() {
  const headers = { Accept: 'application/json', 'X-Request-Id': `k6-${__VU}-${__ITER}` };
  for (const spec of PATHS) {
    const res = http.get(`${BASE_URL}${spec.path}`, {
      headers,
      tags: { endpoint: spec.name, scenario: SCENARIO },
    });
    spec.trend.add(res.timings.duration);
    const ok = res.status === 200;
    non200.add(!ok);
    if (ok) {
      successes.add(1);
    }
    check(res, {
      [`${spec.name} is 200`]: (r) => r.status === 200,
    });
  }
}

export function warmup() {
  hitCatalogue();
  sleep(0.5);
}

export function measured() {
  hitCatalogue();
  sleep(SCENARIO === 'burst' ? 0.2 : 1);
}
