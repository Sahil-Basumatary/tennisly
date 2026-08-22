import exec from 'k6/execution';
import http from 'k6/http';
import ws from 'k6/ws';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:18094').replace(/\/$/, '');
const WS_URL = __ENV.WS_URL || BASE_URL.replace(/^http/, 'ws') + '/ws/matches';
const WS_URLS = (__ENV.WS_URLS || WS_URL)
  .split(',')
  .map((url) => url.trim())
  .filter(Boolean);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const WORKER_INDEX = Number(__ENV.K6_WORKER_INDEX || '0');
const WS_CLIENTS = Number(__ENV.WS_CLIENTS || 100);
const MATCH_COUNT = Number(__ENV.MATCH_COUNT || 8);
const WS_DURATION = __ENV.WS_DURATION || '20s';
const WRITER_START = __ENV.WRITER_START || '3s';
const SUBSCRIBER_ITERATIONS = Number(__ENV.SUBSCRIBER_ITERATIONS || 1);
const SUBSCRIBER_MAX_DURATION = __ENV.SUBSCRIBER_MAX_DURATION || '30s';
const SUBSCRIBER_RAMP = __ENV.SUBSCRIBER_RAMP || '0s';
const WS_HOLD_MS = Number(__ENV.WS_HOLD_MS || 25000);
const RECONNECT_PAUSE_MS = Number(__ENV.RECONNECT_PAUSE_MS || 100);
const POINT_INTERVAL_MS = Number(__ENV.POINT_INTERVAL_MS || 250);
const WARMUP_POINTS = Number(__ENV.WARMUP_POINTS || 5);
const SLOW_CLIENT_PERCENT = Number(__ENV.SLOW_CLIENT_PERCENT || 0);
const SLOW_CLIENT_DELAY_MS = Number(__ENV.SLOW_CLIENT_DELAY_MS || 100);
const DELIVERY_P99_MS = Number(__ENV.DELIVERY_P99_MS || 50);
const REPLAY_ON_RECONNECT = (__ENV.REPLAY_ON_RECONNECT || 'true') === 'true';
const REQUIRE_REPLAY = (__ENV.REQUIRE_REPLAY || 'false') === 'true';
const REQUIRE_FULL_CONNECT = (__ENV.REQUIRE_FULL_CONNECT || 'true') === 'true';
const RAMP_ENABLED = SUBSCRIBER_RAMP !== '0' && SUBSCRIBER_RAMP !== '0s' && SUBSCRIBER_RAMP !== '0ms';

const deliveryLatency = new Trend('live_ws_delivery_ms', true);
const connectLatency = new Trend('live_ws_connect_ms', true);
const connectSucceeded = new Rate('live_ws_connected');
const connectFailed = new Counter('live_ws_connect_failed');
const subscribed = new Counter('live_ws_subscribed');
const writeFailed = new Rate('live_ws_write_failed');
const replayFailed = new Rate('live_ws_replay_failed');
const messagesReceived = new Counter('live_ws_messages');
const sequenceGaps = new Counter('live_ws_sequence_gaps');
const unrecoveredGaps = new Counter('live_ws_unrecovered_gaps');
const recoveredEvents = new Counter('live_ws_recovered_events');
const duplicates = new Counter('live_ws_duplicates');
const malformedFrames = new Counter('live_ws_malformed_frames');
const plannedReconnects = new Counter('live_ws_planned_reconnects');
const failovers = new Counter('live_ws_failovers');
const slowMessages = new Counter('live_ws_slow_messages');
const REQUIRE_NODE_TRAFFIC = (__ENV.REQUIRE_NODE_TRAFFIC || 'true') === 'true';

const writerScenarios = {};
const nodeMessageThresholds = {};

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
for (let index = 0; index < WS_URLS.length; index += 1) {
  if (REQUIRE_NODE_TRAFFIC) {
    nodeMessageThresholds[`live_ws_messages{node:${index}}`] = ['count>0'];
  }
}

const subscriberScenario = RAMP_ENABLED
  ? {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: SUBSCRIBER_RAMP, target: WS_CLIENTS },
        { duration: `${Math.max(1, Math.ceil(WS_HOLD_MS / 1000))}s`, target: WS_CLIENTS },
      ],
      gracefulRampDown: '5s',
      gracefulStop: '5s',
      exec: 'subscriber',
    }
  : {
      executor: 'per-vu-iterations',
      vus: WS_CLIENTS,
      iterations: SUBSCRIBER_ITERATIONS,
      maxDuration: SUBSCRIBER_MAX_DURATION,
      exec: 'subscriber',
      gracefulStop: '5s',
    };

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    subscribers: subscriberScenario,
    ...writerScenarios,
  },
  thresholds: {
    live_ws_connected: [REQUIRE_FULL_CONNECT ? 'rate==1' : 'rate>0.99'],
    live_ws_connect_failed: ['count==0'],
    live_ws_write_failed: ['rate<0.001'],
    'live_ws_delivery_ms{client:normal}': [`p(99)<${DELIVERY_P99_MS}`],
    live_ws_messages: ['count>0'],
    live_ws_unrecovered_gaps: ['count==0'],
    live_ws_duplicates: ['count==0'],
    live_ws_malformed_frames: ['count==0'],
    live_ws_replay_failed: ['rate<0.001'],
    ...(REQUIRE_REPLAY ? { live_ws_recovered_events: ['count>0'] } : {}),
    ...nodeMessageThresholds,
  },
};

function uuid(group, index) {
  return `00000000-0000-4000-${group}-${index.toString(16).padStart(12, '0')}`;
}

function playerIdOnSide(match, side) {
  const players = Array.isArray(match.players) ? match.players : [];
  const found = players.find((player) => player.side === side);
  return found ? found.playerId : null;
}

function joinExistingMatch(externalId) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    const snapshot = http.get(`${BASE_URL}/api/matches/external/${encodeURIComponent(externalId)}`, {
      tags: { phase: 'setup-join' },
    });
    if (snapshot.status === 200) {
      const body = snapshot.json();
      const homeId = playerIdOnSide(body, 'HOME');
      const awayId = playerIdOnSide(body, 'AWAY');
      const liveSequence = Number(body.liveSequence);
      if (body.status === 'IN_PROGRESS' && homeId && awayId && Number.isSafeInteger(liveSequence)) {
        return { matchId: body.id, homeId, awayId, initialSequence: liveSequence };
      }
    }
    sleep(0.25);
  }
  fail(`timed out joining match ${externalId}`);
}

function createMatch(index) {
  const homeId = uuid('a000', index + 1);
  const awayId = uuid('b000', index + 1);
  const externalId = `ws-perf-${RUN_ID}-${index}`;
  const create = http.post(
    `${BASE_URL}/api/matches`,
    JSON.stringify({
      externalId,
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
  if (create.status === 409) {
    return joinExistingMatch(externalId);
  }
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
  if (WORKER_INDEX > 0) {
    for (let index = 0; index < MATCH_COUNT; index += 1) {
      matches.push(joinExistingMatch(`ws-perf-${RUN_ID}-${index}`));
    }
    return { matches };
  }
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

function applySequence(state, sequence, source) {
  if (sequence < state.cursor) {
    duplicates.add(1);
    return 'duplicate';
  }
  if (sequence === state.cursor) {
    return 'overlap';
  }
  if (sequence > state.cursor + 1) {
    if (source === 'replay') {
      const missing = sequence - state.cursor - 1;
      sequenceGaps.add(missing);
      unrecoveredGaps.add(missing);
      state.cursor = sequence;
      return 'gap';
    }
    // Redis is at-most-once; live jumps wait for HTTP replay instead of counting as loss.
    return 'ahead';
  }
  state.cursor = sequence;
  return 'applied';
}

function replayMissed(match, state) {
  let after = state.cursor;
  for (let page = 0; page < 32; page += 1) {
    const response = http.get(
      `${BASE_URL}/api/matches/${match.matchId}/events?afterSequence=${after}&limit=1000`,
      { tags: { phase: 'replay', matchId: match.matchId } },
    );
    const ok = check(response, { 'replay 200': (result) => result.status === 200 });
    replayFailed.add(!ok);
    if (!ok) {
      return;
    }
    const events = response.json();
    if (!Array.isArray(events) || events.length === 0) {
      return;
    }
    for (const event of events) {
      const sequence = Number(event.sequence);
      if (!Number.isSafeInteger(sequence)) {
        malformedFrames.add(1);
        continue;
      }
      const previous = state.cursor;
      const result = applySequence(state, sequence, 'replay');
      if (result === 'applied' && sequence === previous + 1) {
        recoveredEvents.add(1);
      }
    }
    after = state.cursor;
    if (events.length < 1000) {
      return;
    }
  }
}

function recordLiveMessage(state, body, slowClient, node) {
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
  const result = applySequence(state, sequence, 'live');
  if (result !== 'applied') {
    return;
  }
  const client = slowClient ? 'slow' : 'normal';
  messagesReceived.add(1, { node, client });
  deliveryLatency.add(Math.max(0, Date.now() - committedAt), { node, client });
  if (slowClient) {
    slowMessages.add(1);
    sleep(SLOW_CLIENT_DELAY_MS / 1000);
  }
}

let vuState;

export function subscriber(data) {
  const match = data.matches[(__VU - 1) % data.matches.length];
  const preferred = (__VU - 1) % WS_URLS.length;
  const slowClient = (__VU % 100) < SLOW_CLIENT_PERCENT;
  const client = slowClient ? 'slow' : 'normal';
  if (!vuState) {
    vuState = { cursor: match.initialSequence };
  }
  const state = vuState;
  const willReconnect = !RAMP_ENABLED && __ITER < SUBSCRIBER_ITERATIONS - 1;
  // Catch up before CONNECT so replay HTTP cannot stall STOMP delivery samples.
  if (REPLAY_ON_RECONNECT && __ITER > 0) {
    replayMissed(match, state);
  }
  let stompConnected = false;
  let subscribedThisAttempt = false;
  const holdMs = RAMP_ENABLED ? 24 * 60 * 60 * 1000 : WS_HOLD_MS;
  for (let shift = 0; shift < WS_URLS.length; shift += 1) {
    const node = (preferred + shift) % WS_URLS.length;
    const wsUrl = WS_URLS[node];
    const startedAt = Date.now();
    let connectedThisAttempt = false;
    const response = ws.connect(
      wsUrl,
      {
        headers: { Origin: BASE_URL },
        tags: {
          matchId: match.matchId,
          node: String(node),
          client,
        },
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
              connectedThisAttempt = true;
              stompConnected = true;
              connectLatency.add(Date.now() - startedAt);
              socket.send(
                stompFrame('SUBSCRIBE', {
                  id: `sub-${__VU}-${match.matchId}`,
                  destination: `/topic/matches/${match.matchId}`,
                  ack: 'auto',
                }),
              );
              subscribedThisAttempt = true;
            } else if (frame.command === 'MESSAGE') {
              recordLiveMessage(state, frame.body, slowClient, String(node));
            }
          }
        });
        socket.setTimeout(() => {
          if (willReconnect) {
            plannedReconnects.add(1);
          }
          socket.send(stompFrame('DISCONNECT'));
          socket.close();
        }, holdMs);
      },
    );
    if (response && response.status === 101 && connectedThisAttempt) {
      if (shift > 0) {
        failovers.add(1);
      }
      break;
    }
  }
  connectSucceeded.add(stompConnected);
  if (!stompConnected) {
    connectFailed.add(1);
  } else if (subscribedThisAttempt) {
    subscribed.add(1);
  }
  // Fill any live-ahead hole after the socket is gone so ignored jumps cannot hide loss.
  if (REPLAY_ON_RECONNECT && stompConnected) {
    replayMissed(match, state);
  }
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
