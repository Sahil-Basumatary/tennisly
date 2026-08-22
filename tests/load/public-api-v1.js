import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Metered /api/v1 catalogue. Requires a live API key and warm user-service.
 * Keep this off the public Starter SLO until user-service is always-on.
 *
 *   API_KEY=tly_live_... BASE_URL=http://localhost:8080 k6 run tests/load/public-api-v1.js
 */

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const API_KEY = __ENV.API_KEY || '';

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: 1,
      duration: '10s',
      exec: 'warmup',
    },
    measured: {
      executor: 'constant-vus',
      vus: 1,
      duration: '20s',
      startTime: '10s',
      exec: 'measured',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['p(95)<250', 'p(99)<500'],
  },
};

const PATHS = [
  '/api/v1/players?gender=MALE',
  '/api/v1/rankings?gender=MALE&type=SINGLES',
  '/api/v1/matches?status=IN_PROGRESS',
];

function hit() {
  if (!API_KEY) {
    throw new Error('API_KEY is required for /api/v1');
  }
  const headers = { Accept: 'application/json', 'X-Api-Key': API_KEY };
  for (const path of PATHS) {
    const res = http.get(`${BASE_URL}${path}`, { headers, tags: { path } });
    check(res, { [`${path} is 200`]: (r) => r.status === 200 });
  }
}

export function warmup() {
  hit();
  sleep(0.5);
}

export function measured() {
  hit();
  sleep(1);
}
