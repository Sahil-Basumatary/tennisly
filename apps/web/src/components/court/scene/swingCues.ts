import type { ReplayFrame, ShotSummary, ShotType } from "@/types/replay";
import type { SwingClip } from "./PlayerAnimator";

export type SwingCue = {
  /** Replay time the ball is struck. */
  timeSeconds: number;
  clip: SwingClip;
  side: "home" | "away";
};

/**
 * Pairs each shot with the replay time it is struck, so swings can be driven
 * off the clock rather than edge-triggered on a shot-index change. Time-driven
 * cues survive scrubbing and reverse seeking, which edge triggers do not.
 */
export function buildSwingCues(frames: ReplayFrame[], shots: ShotSummary[]): SwingCue[] {
  const contactTime = new Map<number, number>();
  for (const frame of frames) {
    if (!contactTime.has(frame.shotIndex)) {
      contactTime.set(frame.shotIndex, frame.timeSeconds);
    }
  }
  const cues: SwingCue[] = [];
  for (const shot of shots) {
    const timeSeconds = contactTime.get(shot.shotIndex);
    if (timeSeconds === undefined) continue;
    cues.push({
      timeSeconds,
      clip: shotTypeToClip(shot.shotType),
      side: shot.hitter === "HOME" ? "home" : "away",
    });
  }
  return cues.sort((a, b) => a.timeSeconds - b.timeSeconds);
}

export function shotTypeToClip(shot: ShotType): SwingClip {
  if (shot.includes("SERVE")) return "serve";
  if (shot.includes("SMASH") || shot.includes("OVERHEAD")) return "smash";
  if (shot.includes("BACKHAND")) return "backhand";
  return "forehand";
}
