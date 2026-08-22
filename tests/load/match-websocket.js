import exec from 'k6/execution';
import http from 'k6/http';
import ws from 'k6/ws';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:18094').replace(/\/$/, '');
const WS_URL = __ENV.WS_URL || BASE_URL.replace(/^http/, 'ws') + '/ws/matches';
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const WS_CLIENTS = Number(__ENV.WS_CLIENTS || 100);
const MATCH_COUNT = Number(__ENV.MATCH_COUNT || 8);
const WS_DURATION = __ENV.WS_DURATION || '20s';
const WRITER_START = __ENV.WRITER_START || '3s';
const SUBSCRIBER_ITERATIONS = Number(__ENV.SUBSCRIBER_ITERATIONS || 1);
const SUBSCRIBER_MAX_DURATION = __ENV.SUBSCRIBER_MAX_DURATION || '30s';
const WS_HOLD_MS = Number(__ENV.WS_HOLD_MS || 25000);
const RECONNECT_PAUSE_MS = Number(__ENV.RECONNECT_PAUSE_MS || 100);
const POINT_INTERVAL_MS = Number(__ENV.POINT_INTERVAL_MS || 250);
const WARMUP_POINTS = Number(__ENV.WARMUP_POINTS || 5);
const SLOW_CLIENT_PERCENT = Number(__ENV.SLOW_CLIENT_PERCENT || 0);
const SLOW_CLIENT_DELAY_MS = Number(__ENV.SLOW_CLIENT_DELAY_MS || 100);
const DELIVERY_P99_MS = Number(__ENV.DELIVERY_P99_MS || 50);

const deliveryLatency = new Trend('live_ws_delivery_ms', true);
const connectLatency = new Trend('live_ws_connect_ms', true);
const connectSucceeded = new Rate('live_ws_connected');
const writeFailed = new Rate('live_ws_write_failed');
const messagesReceived = new Counter('live_ws_messages');
const sequenceGaps = new Counter('live_ws_sequence_gaps');
const duplicates = new Counter('live_ws_duplicates');
const malformedFrames = new Counter('live_ws_malformed_frames');
const plannedReconnects = new Counter('live_ws_planned_reconnects');
const slowMessages = new Counter('live_ws_slow_messages');

const lastSequenceByMatch = {};
const writerScenarios = {};

for (let index = 0; index < MATCH_COUNT; index += 1) {
  writerScenarios[`writer_${index}`] = {
    executor: 'constant-vus',
    vus: 1,
    duration: WS_DURATION,
    startTime: WRITER_START,
    exec: 'writer',
    gracefulStop: '5s',
    env: { MATCH_INDEX: String(index) },
  };
}

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    subscribers: {
      executor: 'per-vu-iterations',
      vus: WS_CLIENTS,
      iterations: SUBSCRIBER_ITERATIONS,
      maxDuration: SUBSCRIBER_MAX_DURATION,
      exec: 'subscriber',
      gracefulStop: '5s',
    },
    ...writerScenarios,
  },
  thresholds: {
    live_ws_connected: ['rate>0.999'],
    live_ws_write_failed: ['rate<0.001'],
    live_ws_delivery_ms: [`p(99)<${DELIVERY_P99_MS}`],
    live_ws_messages: ['count>0'],
    live_ws_sequence_gaps: ['count==0'],
    live_ws_duplicates: ['count==0'],
    live_ws_malformed_frames: ['count==0'],
  },
};

function uuid(group, index) {
  return `00000000-0000-4000-${group}-${index.toString(16).padStart(12, '0')}`;
}

function createMatch(index) {
  const homeId = uuid('a000', index + 1);
  const awayId = uuid('b000', index + 1);
  const create = http.post(
    `${BASE_URL}/api/matches`,
    JSON.stringify({
      externalId: `ws-perf-${RUN_ID}-${index}`,
      surface: 'HARD',
      bestOfSets: 3,
      metadata: { benchmark: 'websocket', runId: RUN_ID },
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
  for (let warmup = 0; warmup < WARMUP_POINTS; warmup += 1) {
    const playerId = warmup % 2 === 0 ? homeId : awayId;
    const point = http.post(
      `${BASE_URL}/api/matches/${matchId}/points`,
      JSON.stringify({
        serverId: playerId,
        winnerId: playerId,
        outcome: 'WINNER',
        rallyLength: 4,
        scoreSnapshot: { points: ['0', '0'], warmup },
        shotSummary: {},
      }),
      { headers: { 'Content-Type': 'application/json' }, tags: { phase: 'setup-warmup' } },
    );
    if (point.status !== 201) {
      fail(`warm-up point failed status=${point.status} body=${point.body}`);
    }
  }
  const snapshot = http.get(`${BASE_URL}/api/matches/${matchId}`, {
    tags: { phase: 'setup-warmup' },
  });
  if (snapshot.status !== 200) {
    fail(`warm-up snapshot failed status=${snapshot.status} body=${snapshot.body}`);
  }
  return {
    matchId,
    homeId,
    awayId,
    initialSequence: Number(snapshot.json('liveSequence')),
  };
}

export function setup() {
  if (!Number.isInteger(MATCH_COUNT) || MATCH_COUNT < 1) {
    fail('MATCH_COUNT must be a positive integer');
  }
  if (!Number.isInteger(WARMUP_POINTS) || WARMUP_POINTS < 0) {
    fail('WARMUP_POINTS must be a non-negative integer');
  }
  const matches = [];
  for (let index = 0; index < MATCH_COUNT; index += 1) {
    matches.push(createMatch(index));
  }
  return { matches };
}

function stompFrame(command, headers = {}, body = '') {
  const lines = [command];
  for (const [name, value] of Object.entries(headers)) {
    lines.push(`${name}:${value}`);
  }
  lines.push('', body);
  return `${lines.join('\n')}\0`;
}

function parseFrames(message) {
  const frames = [];
  for (const raw of String(message).split('\0')) {
    const normalized = raw.replace(/^\n+/, '');
    if (!normalized.trim()) {
      continue;
    }
    const separator = normalized.indexOf('\n\n');
    const headerBlock = separator >= 0 ? normalized.slice(0, separator) : normalized;
    const body = separator >= 0 ? normalized.slice(separator + 2) : '';
    const lines = headerBlock.split('\n');
    const command = lines.shift();
    const headers = {};
    for (const line of lines) {
      const colon = line.indexOf(':');
      if (colon > 0) {
        headers[line.slice(0, colon)] = line.slice(colon + 1);
      }
    }
    frames.push({ command, headers, body });
  }
  return frames;
}

function recordLiveMessage(match, body, slowClient) {
  let event;
  try {
    event = JSON.parse(body);
  } catch (_error) {
    malformedFrames.add(1);
    return;
  }
  const sequence = Number(event.sequence);
  const committedAt = Date.parse(event.commitObservedAt);
  if (!Number.isSafeInteger(sequence) || !Number.isFinite(committedAt)) {
    malformedFrames.add(1);
    return;
  }
  const previous = lastSequenceByMatch[match.matchId] ?? match.initialSequence;
  if (sequence <= previous) {
    duplicates.add(1);
    return;
  }
  if (sequence > previous + 1) {
    sequenceGaps.add(sequence - previous - 1);
  }
  lastSequenceByMatch[match.matchId] = sequence;
  messagesReceived.add(1);
  deliveryLatency.add(Math.max(0, Date.now() - committedAt));
  if (slowClient) {
    slowMessages.add(1);
    sleep(SLOW_CLIENT_DELAY_MS / 1000);
  }
}

export function subscriber(data) {
  const match = data.matches[(__VU - 1) % data.matches.length];
  const slowClient = (__VU % 100) < SLOW_CLIENT_PERCENT;
  const startedAt = Date.now();
  let stompConnected = false;
  const willReconnect = __ITER < SUBSCRIBER_ITERATIONS - 1;
  const response = ws.connect(
    WS_URL,
    {
      headers: { Origin: BASE_URL },
      tags: { matchId: match.matchId, client: slowClient ? 'slow' : 'normal' },
    },
    (socket) => {
      socket.on('open', () => {
        socket.send(
          stompFrame('CONNECT', {
            'accept-version': '1.2',
            'heart-beat': '10000,10000',
            host: 'localhost',
          }),
        );
      });
      socket.on('message', (message) => {
        for (const frame of parseFrames(message)) {
          if (frame.command === 'CONNECTED') {
            stompConnected = true;
            connectLatency.add(Date.now() - startedAt);
            socket.send(
              stompFrame('SUBSCRIBE', {
                id: `sub-${__VU}-${match.matchId}`,
                destination: `/topic/matches/${match.matchId}`,
                ack: 'auto',
              }),
            );
          } else if (frame.command === 'MESSAGE') {
            recordLiveMessage(match, frame.body, slowClient);
          } else if (frame.command === 'ERROR') {
            malformedFrames.add(1);
          }
        }
      });
      socket.setTimeout(() => {
        if (willReconnect) {
          plannedReconnects.add(1);
        }
        socket.send(stompFrame('DISCONNECT'));
        socket.close();
      }, WS_HOLD_MS);
    },
  );
  connectSucceeded.add(response && response.status === 101 && stompConnected);
  if (willReconnect) {
    sleep(RECONNECT_PAUSE_MS / 1000);
  }
}

export function writer(data) {
  const matchIndex = Number(__ENV.MATCH_INDEX);
  const match = data.matches[matchIndex];
  const homeWins = (exec.scenario.iterationInTest + exec.vu.idInInstance) % 2 === 0;
  const playerId = homeWins ? match.homeId : match.awayId;
  const response = http.post(
    `${BASE_URL}/api/matches/${match.matchId}/points`,
    JSON.stringify({
      serverId: playerId,
      winnerId: playerId,
      outcome: 'WINNER',
      rallyLength: 4,
      scoreSnapshot: {
        points: homeWins ? ['15', '0'] : ['0', '15'],
        benchmarkIteration: exec.scenario.iterationInTest,
      },
      shotSummary: {},
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `ws-${RUN_ID}-${matchIndex}-${exec.scenario.iterationInTest}`,
      },
      tags: { phase: 'writer', matchId: match.matchId },
    },
  );
  const ok = check(response, { 'point committed with 201': (result) => result.status === 201 });
  writeFailed.add(!ok);
  if (POINT_INTERVAL_MS > 0) {
    sleep(POINT_INTERVAL_MS / 1000);
  }
}
