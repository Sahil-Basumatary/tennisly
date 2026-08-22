import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:18094').replace(/\/$/, '');
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const WRITE_VUS = Number(__ENV.WRITE_VUS || 8);
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 2);
const WRITE_DURATION = __ENV.WRITE_DURATION || '30s';
const P95_BUDGET_MS = Number(__ENV.WRITE_P95_MS || 100);
const P99_BUDGET_MS = Number(__ENV.WRITE_P99_MS || 250);

const commitDuration = new Trend('durable_point_commit_ms', true);
const non201 = new Rate('durable_point_non_201');
const committed = new Counter('durable_points_committed');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: WARMUP_VUS,
      duration: '10s',
      exec: 'warmup',
      gracefulStop: '10s',
    },
    measured: {
      executor: 'constant-vus',
      vus: WRITE_VUS,
      duration: WRITE_DURATION,
      startTime: '10s',
      exec: 'measured',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    durable_point_non_201: ['rate<0.001'],
    durable_point_commit_ms: [`p(95)<${P95_BUDGET_MS}`, `p(99)<${P99_BUDGET_MS}`],
  },
};

function uuid(group, index) {
  return `00000000-0000-4000-${group}-${index.toString(16).padStart(12, '0')}`;
}

function createMatch(kind, index) {
  const homeId = uuid('8000', index + 1);
  const awayId = uuid('9000', index + 1);
  const externalId = `perf-${RUN_ID}-${kind}-${index}`;
  const create = http.post(
    `${BASE_URL}/api/matches`,
    JSON.stringify({
      externalId,
      surface: 'HARD',
      bestOfSets: 3,
      metadata: { benchmark: 'durable-write', runId: RUN_ID },
      players: [
        { playerId: homeId, displayName: `Home ${index}`, side: 'HOME' },
        { playerId: awayId, displayName: `Away ${index}`, side: 'AWAY' },
      ],
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { phase: 'setup' } },
  );
  if (create.status !== 201) {
    fail(`create match failed status=${create.status} body=${create.body}`);
  }
  const matchId = create.json('id');
  const status = http.patch(
    `${BASE_URL}/api/matches/${matchId}/status`,
    JSON.stringify({ status: 'IN_PROGRESS', metadata: {} }),
    { headers: { 'Content-Type': 'application/json' }, tags: { phase: 'setup' } },
  );
  if (status.status !== 200) {
    fail(`start match failed status=${status.status} body=${status.body}`);
  }
  return { matchId, homeId, awayId };
}

export function setup() {
  const warmupMatches = [];
  const measuredMatches = [];
  const globalVuSlots = WRITE_VUS + WARMUP_VUS;
  // k6 assigns global VU IDs independently of scenario size, so both pools cover every slot.
  for (let index = 0; index < globalVuSlots; index += 1) {
    warmupMatches.push(createMatch('warmup', index));
    measuredMatches.push(createMatch('measured', index));
  }
  return { warmupMatches, measuredMatches };
}

function recordPoint(matches, measured) {
  const match = matches[__VU - 1];
  if (!match) {
    fail(`no dedicated match for VU ${__VU}`);
  }
  const homeWins = (__ITER + __VU) % 2 === 0;
  const serverId = homeWins ? match.homeId : match.awayId;
  const winnerId = homeWins ? match.homeId : match.awayId;
  const response = http.post(
    `${BASE_URL}/api/matches/${match.matchId}/points`,
    JSON.stringify({
      serverId,
      winnerId,
      outcome: 'WINNER',
      rallyLength: 4,
      scoreSnapshot: { points: ['15', '0'], benchmarkSequence: __ITER },
      shotSummary: {},
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `durable-${RUN_ID}-${__VU}-${__ITER}`,
      },
      tags: { phase: measured ? 'measured' : 'warmup' },
    },
  );
  const ok = check(response, { 'point committed with 201': (result) => result.status === 201 });
  if (measured) {
    non201.add(!ok);
    if (ok) {
      commitDuration.add(response.timings.duration);
      committed.add(1);
    }
  }
}

export function warmup(data) {
  recordPoint(data.warmupMatches, false);
}

export function measured(data) {
  recordPoint(data.measuredMatches, true);
}
