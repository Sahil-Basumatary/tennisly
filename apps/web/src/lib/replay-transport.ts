import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

export function indexAtOrBefore(starts: number[], timeSeconds: number): number {
  let current = 0;
  for (let i = 0; i < starts.length; i++) {
    if (timeSeconds + 1e-3 >= (starts[i] ?? 0)) current = i;
  }
  return current;
}

/** Seek to shot ±1 and sync the active shot chip. */
export function stepShot(delta: number): void {
  const { timeSeconds, seek } = usePlayback.getState();
  const { shotStartTimes, setActiveShotIndex } = useReplaySession.getState();
  if (shotStartTimes.length === 0) return;
  const current = indexAtOrBefore(shotStartTimes, timeSeconds);
  const next = Math.max(0, Math.min(shotStartTimes.length - 1, current + delta));
  seek(shotStartTimes[next] ?? 0);
  setActiveShotIndex(next);
}

/** Seek to point ±1 start time. */
export function stepPoint(delta: number): void {
  const { timeSeconds, seek } = usePlayback.getState();
  const { pointStartTimes } = useReplaySession.getState();
  if (pointStartTimes.length === 0) return;
  const current = indexAtOrBefore(pointStartTimes, timeSeconds);
  const next = Math.max(0, Math.min(pointStartTimes.length - 1, current + delta));
  seek(pointStartTimes[next] ?? 0);
}

/** Wall-clock nudge on the scrubber (±seconds). */
export function nudgeTime(deltaSeconds: number): void {
  const { timeSeconds, seek } = usePlayback.getState();
  seek(timeSeconds + deltaSeconds);
}
