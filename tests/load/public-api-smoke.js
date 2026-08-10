import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Smoke load against the public API catalogue paths.
 *
 *   export BASE_URL=http://localhost:8080
 *   export API_KEY=tly_live_...
 *   k6 run tests/load/public-api-smoke.js
 */
export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || '';

const PATHS = [
  '/api/v1/players?page=0&size=5',
  '/api/v1/rankings?page=0&size=5',
  '/api/v1/matches?page=0&size=5',
];

export default function () {
  const headers = API_KEY ? { 'X-Api-Key': API_KEY } : {};
  for (const path of PATHS) {
    const res = http.get(`${BASE_URL}${path}`, { headers });
    check(res, {
      [`${path} is 200 or auth failure`]: (r) =>
        r.status === 200 || r.status === 401 || r.status === 403,
    });
  }
  sleep(1);
}
