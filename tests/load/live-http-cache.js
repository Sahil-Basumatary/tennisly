import exec from 'k6/execution';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://127.0.0.1:18081').replace(/\/$/, '');
const MATCH_ID = __ENV.MATCH_ID || 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee';
const CLIENTS = Number(__ENV.HTTP_LIVE_CLIENTS || 100);
const HOLD_S = Number(__ENV.HTTP_LIVE_HOLD_S || 25);
const RAMP_S = Number(__ENV.HTTP_LIVE_RAMP_S || 3);
const POLL_S = Number(__ENV.HTTP_LIVE_POLL_S || 3);
const TICKER_EVERY = Number(__ENV.HTTP_TICKER_EVERY || 3);
const CACHE_HIT_MIN = Number(
  __ENV.HTTP_LIVE_CACHE_HIT_MIN || (CLIENTS >= 1000 ? 0.997 : 0.98),
);

const cacheHits = new Counter('http_live_cache_hits');
const cacheMisses = new Counter('http_live_cache_misses');
const originBypass = new Counter('http_live_origin_bypass');
const notModified = new Counter('http_live_not_modified');
const viewerBytes = new Counter('http_live_viewer_bytes');
const unrecoveredGaps = new Counter('http_live_unrecovered_gaps');
const recoveredEvents = new Counter('http_live_recovered_events');
const duplicates = new Counter('http_live_duplicates');
const staleAge = new Trend('http_live_stale_age_s', false);
const viewerLatency = new Trend('http_live_viewer_ms', true);
const cacheHitRate = new Rate('http_live_cache_hit_rate');
const recoveryRate = new Rate('http_live_recovery_rate');

export const options = {
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    viewers: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: `${RAMP_S}s`, target: CLIENTS },
        { duration: `${HOLD_S}s`, target: CLIENTS },
      ],
      gracefulRampDown: '3s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_live_unrecovered_gaps: ['count==0'],
    http_live_duplicates: ['count==0'],
    http_live_recovery_rate: ['rate<0.001'],
    http_live_cache_hit_rate: [`rate>${CACHE_HIT_MIN}`],
  },
};

function needsEventRecovery(cursor, liveSequence) {
  if (!Number.isFinite(liveSequence) || liveSequence <= 0) return false;
  if (!Number.isFinite(cursor) || cursor <= 0) return false;
  return liveSequence > cursor + 1;
}

function jitterSleep() {
  const jitter = Math.random() * 0.4;
  sleep(POLL_S + jitter);
}

function readCache(response, name) {
  if (response.status === 0) {
    cacheHitRate.add(false);
    return;
  }
  const header = String(response.headers['X-Cache'] || response.headers['x-cache'] || '');
  const age = Number(response.headers['Age'] || response.headers['age'] || 0);
  if (Number.isFinite(age)) staleAge.add(age);
  viewerBytes.add(response.body ? response.body.length : 0);
  viewerLatency.add(response.timings.duration);
  if (name === 'events') {
    originBypass.add(1);
    return;
  }
  if (header === 'HIT') {
    cacheHits.add(1);
    cacheHitRate.add(true);
  } else {
    cacheMisses.add(1);
    cacheHitRate.add(false);
  }
  if (response.status === 304) notModified.add(1);
}

export default function viewer() {
  let cursor = 0;
  const seen = {};
  let etag = '';
  let polls = 0;
  while (true) {
    const headers = { Accept: 'application/json' };
    if (etag) headers['If-None-Match'] = etag;
    const live = http.get(`${BASE_URL}/api/matches/${MATCH_ID}/cursor`, { headers, tags: { name: 'cursor' } });
    readCache(live, 'cursor');
    check(live, {
      'cursor 200 or 304': (res) => res.status === 200 || res.status === 304,
    });
    let recoveredThisPoll = false;
    if (live.status === 200) {
      const nextTag = live.headers['ETag'] || live.headers['etag'];
      if (nextTag) etag = nextTag;
      let body = {};
      try {
        body = JSON.parse(live.body);
      } catch (err) {
        body = {};
      }
      const sequence = Number(body.liveSequence || 0);
      if (needsEventRecovery(cursor, sequence)) {
        recoveredThisPoll = true;
        const events = http.get(
          `${BASE_URL}/api/matches/${MATCH_ID}/events?afterSequence=${cursor}&limit=1000`,
          { headers: { Accept: 'application/json' }, tags: { name: 'events' } },
        );
        readCache(events, 'events');
        if (events.status === 200) {
          let rows = [];
          try {
            rows = JSON.parse(events.body);
          } catch (err) {
            rows = [];
          }
          recoveredEvents.add(Array.isArray(rows) ? rows.length : 0);
          let recovered = cursor;
          for (const row of Array.isArray(rows) ? rows : []) {
            const value = Number(row.sequence);
            if (!Number.isFinite(value)) continue;
            if (seen[value]) duplicates.add(1);
            seen[value] = true;
            if (value > recovered) recovered = value;
          }
          if (recovered < sequence) unrecoveredGaps.add(1);
          cursor = Math.max(recovered, sequence);
        } else {
          unrecoveredGaps.add(1);
        }
      } else if (sequence > cursor) {
        seen[sequence] = true;
        cursor = sequence;
      }
    }
    recoveryRate.add(recoveredThisPoll);
    polls += 1;
    if (polls % TICKER_EVERY === 0) {
      const ticker = http.get(`${BASE_URL}/api/matches/ticker`, {
        headers: { Accept: 'application/json' },
        tags: { name: 'ticker' },
      });
      readCache(ticker, 'ticker');
      check(ticker, { 'ticker ok': (res) => res.status === 200 || res.status === 304 });
    }
    if (exec.scenario.progress >= 0.98) {
      break;
    }
    jitterSleep();
  }
}
