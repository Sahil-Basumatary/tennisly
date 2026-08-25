import type { PointReplay, PointSummary, ReplayFrame, ShotSummary } from "@/types/replay";

export const LIVE_POINT_BUFFER = 48;

export type ReplayTape = {
  frames: ReplayFrame[];
  shots: ShotSummary[];
  points: PointSummary[];
};

function shotKey(pointSequence: number, shotIndex: number): string {
  return `${pointSequence}:${shotIndex}`;
}

/** Engine shotIndex restarts each point; transport needs a dense match-level index. */
export function densifyShotIndexes(tape: ReplayTape): ReplayTape {
  const map = new Map<string, number>();
  let next = 0;
  const frames = tape.frames.map((frame) => {
    const key = shotKey(frame.pointSequence, frame.shotIndex);
    let dense = map.get(key);
    if (dense === undefined) {
      dense = next;
      map.set(key, next);
      next += 1;
    }
    return { ...frame, shotIndex: dense };
  });
  const shots = tape.shots
    .map((shot) => {
      const dense = map.get(shotKey(shot.pointSequence, shot.shotIndex));
      if (dense === undefined) return null;
      return { ...shot, shotIndex: dense };
    })
    .filter((shot): shot is ShotSummary => shot !== null)
    .sort((a, b) => a.shotIndex - b.shotIndex);
  return { frames, shots, points: tape.points };
}

export function tapeDurationSeconds(tape: ReplayTape): number {
  if (tape.frames.length === 0) return 0;
  return tape.frames[tape.frames.length - 1]!.timeSeconds;
}

export function trimTapeToLastPoints(tape: ReplayTape, maxPoints: number): ReplayTape {
  if (maxPoints <= 0 || tape.points.length <= maxPoints) return tape;
  const kept = tape.points.slice(-maxPoints);
  const keepSeq = new Set(kept.map((point) => point.sequence));
  let frames = tape.frames.filter((frame) => keepSeq.has(frame.pointSequence));
  const shots = tape.shots.filter((shot) => keepSeq.has(shot.pointSequence));
  const shift = frames[0]?.timeSeconds ?? 0;
  if (shift > 0) {
    frames = frames.map((frame) => ({ ...frame, timeSeconds: frame.timeSeconds - shift }));
  }
  return densifyShotIndexes({ frames, shots, points: kept });
}

export function prepareHydratedTape(tape: ReplayTape, maxPoints?: number): ReplayTape {
  const sorted: ReplayTape = {
    frames: [...tape.frames].sort((a, b) => a.timeSeconds - b.timeSeconds),
    shots: tape.shots,
    points: [...tape.points].sort((a, b) => a.sequence - b.sequence),
  };
  const dense = densifyShotIndexes(sorted);
  return maxPoints ? trimTapeToLastPoints(dense, maxPoints) : dense;
}

export function appendPointToTape(
  existing: ReplayTape,
  incoming: PointReplay,
): ReplayTape | null {
  if (!Number.isSafeInteger(incoming.point.sequence) || incoming.point.sequence < 1) {
    return null;
  }
  if (existing.points.some((point) => point.sequence === incoming.point.sequence)) {
    return null;
  }
  if (!incoming.frames?.length) return null;
  const timeOffset =
    existing.frames.length === 0 ? 0 : existing.frames[existing.frames.length - 1]!.timeSeconds;
  const shotOffset =
    existing.shots.length === 0
      ? 0
      : Math.max(...existing.shots.map((shot) => shot.shotIndex)) + 1;
  const frames = incoming.frames.map((frame) => ({
    ...frame,
    timeSeconds: frame.timeSeconds + timeOffset,
    shotIndex: frame.shotIndex + shotOffset,
  }));
  const shots = incoming.shots.map((shot) => ({
    ...shot,
    shotIndex: shot.shotIndex + shotOffset,
  }));
  return trimTapeToLastPoints(
    densifyShotIndexes({
      frames: [...existing.frames, ...frames],
      shots: [...existing.shots, ...shots],
      points: [...existing.points, incoming.point],
    }),
    LIVE_POINT_BUFFER,
  );
}
