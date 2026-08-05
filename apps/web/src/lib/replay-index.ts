import type { PointSummary, ReplayFrame } from "@/types/replay";

/** First clock time for each dense shotIndex (sparse-safe). */
export function indexShotStartTimes(frames: ReplayFrame[]): number[] {
  const starts: number[] = [];
  for (const frame of frames) {
    if (starts[frame.shotIndex] === undefined) {
      starts[frame.shotIndex] = frame.timeSeconds;
    }
  }
  return starts;
}

/** Start times aligned to `points` order (by sequence), for prev/next point. */
export function indexPointStartTimes(
  frames: ReplayFrame[],
  points: PointSummary[],
): number[] {
  const bySequence = new Map<number, number>();
  for (const frame of frames) {
    if (!bySequence.has(frame.pointSequence)) {
      bySequence.set(frame.pointSequence, frame.timeSeconds);
    }
  }
  return points.map((point) => bySequence.get(point.sequence) ?? 0);
}

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isReplayMatchUuid(matchId: string): boolean {
  return UUID_RE.test(matchId);
}
