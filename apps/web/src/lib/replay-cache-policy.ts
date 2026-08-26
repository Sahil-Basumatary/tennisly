export const REPLAY_ENGINE_VERSION = "2.0.0";

export function isSealedPoint(sequence: number, pointsPlayed: number): boolean {
  return Number.isSafeInteger(sequence) && sequence >= 1 && sequence < pointsPlayed;
}

export function replayEngineMatches(
  requested: string | null | undefined,
  actual: string | null | undefined,
): boolean {
  const want = requested && requested.length > 0 ? requested : REPLAY_ENGINE_VERSION;
  const got = actual && actual.length > 0 ? actual : REPLAY_ENGINE_VERSION;
  return want === got;
}
