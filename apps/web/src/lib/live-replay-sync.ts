import { MISSING_POINT_FETCH_CAP } from "@/lib/live-poll";

type LedgerRow = {
  sequence?: unknown;
  sequenceNumber?: unknown;
};

export function pointSequenceOf(row: LedgerRow): number | null {
  const value = Number(row.sequenceNumber ?? row.sequence);
  return Number.isSafeInteger(value) && value >= 1 ? value : null;
}

export function unseenPointSequences(
  have: Iterable<number>,
  ledger: LedgerRow[],
): number[] {
  const seen = new Set(have);
  const missing: number[] = [];
  for (const row of ledger) {
    const sequence = pointSequenceOf(row);
    if (sequence == null || seen.has(sequence)) continue;
    missing.push(sequence);
    seen.add(sequence);
  }
  return missing.sort((a, b) => a - b);
}

export function missingPointRange(
  have: Iterable<number>,
  pointsPlayed: number,
  cap = MISSING_POINT_FETCH_CAP,
): number[] {
  const seen = new Set(have);
  const missing: number[] = [];
  const last = Math.max(0, pointsPlayed);
  for (let sequence = 1; sequence <= last && missing.length < cap; sequence += 1) {
    if (!seen.has(sequence)) missing.push(sequence);
  }
  return missing;
}

/** Join and one-step advances trust the compact cursor; only a skipped sequence hits /events. */
export function needsEventRecovery(cursor: number, liveSequence: number): boolean {
  if (!Number.isSafeInteger(liveSequence) || liveSequence <= 0) return false;
  if (!Number.isSafeInteger(cursor) || cursor <= 0) return false;
  return liveSequence > cursor + 1;
}

/** After a WS drop, the event feed is the cursor — take the max sequence we can prove. */
export function advanceCursorFromEvents(
  cursor: number,
  events: Array<{ sequence?: unknown }>,
): number {
  let next = cursor;
  for (const event of events) {
    const sequence = Number(event.sequence);
    if (Number.isSafeInteger(sequence) && sequence > next) next = sequence;
  }
  return next;
}

export async function recoverEventCursor(
  matchId: string,
  afterSequence: number,
): Promise<number> {
  let cursor = afterSequence;
  for (let page = 0; page < 32; page += 1) {
    const response = await fetch(
      `/api/matches/${matchId}/events?afterSequence=${cursor}&limit=1000`,
      { cache: "no-store" },
    );
    if (!response.ok) return cursor;
    const events: Array<{ sequence?: unknown }> = await response.json();
    if (!Array.isArray(events) || events.length === 0) return cursor;
    cursor = advanceCursorFromEvents(cursor, events);
    if (events.length < 1000) return cursor;
  }
  return cursor;
}
