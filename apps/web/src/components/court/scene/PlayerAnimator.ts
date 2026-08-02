import type { AnimationGroup } from "@babylonjs/core";
import type { PlayerClip } from "./loadPlayer";

export type SwingClip = "serve" | "forehand" | "backhand" | "smash";

/** Motion Cast clips are authored at 60fps. */
const CLIP_FPS = 60;

/**
 * Frame of peak racket-head speed in each clip, measured by scrubbing the clip
 * and sampling the racket tip. Anchoring contact here is what puts the racket
 * on the ball instead of a beat behind it. Hand speed is the wrong signal: on
 * the forehand it peaks during the backswing loop, three-quarters of a second
 * before the strike.
 */
const CONTACT_FRAME: Record<SwingClip, number> = {
  serve: 166,
  forehand: 158,
  backhand: 96,
  smash: 166,
};

/** Rally shots land roughly a second apart, so only the stroke either side of
 *  contact fits — the clips themselves are 3-5s of mostly idle padding. */
export const SWING_LEAD_SECONDS = 0.85;
export const SWING_FOLLOW_SECONDS = 0.75;

const FADE_SECONDS = 0.18;
const WEIGHT_EPSILON = 0.004;
const JOG_THRESHOLD_MPS = 1.2;
/** Ground speed the jog clip was captured at; rate-scaling around it keeps the
 *  feet roughly planted instead of skating. */
const JOG_REFERENCE_MPS = 3.2;
const JOG_RATE_MIN = 0.65;
const JOG_RATE_MAX = 1.7;

export type SwingPhase = {
  clip: SwingClip;
  /** Negative before contact, positive after. */
  secondsFromContact: number;
};

export type AnimatorInput = {
  swing: SwingPhase | null;
  groundSpeedMps: number;
  /** Replay clock rate: 0 while paused so the athlete freezes with the ball. */
  timeScale: number;
  /** Replay clock jumped (scrub), so a running swing has to be re-anchored. */
  seeked: boolean;
};

const LOOPING: ReadonlySet<PlayerClip> = new Set<PlayerClip>(["idle", "jog"]);

/**
 * Weight-blends the mocap clips instead of hard-cutting between them, and
 * drives swings off the replay clock so contact lands on the frame the ball is
 * actually struck.
 */
export class PlayerAnimator {
  private readonly clips: Partial<Record<PlayerClip, AnimationGroup>>;
  private readonly weights = new Map<PlayerClip, number>();
  private anchoredSwing: PlayerClip | null = null;
  private targetClip: PlayerClip | null = null;
  private paused = false;

  constructor(clips: Partial<Record<PlayerClip, AnimationGroup>>) {
    this.clips = clips;
    for (const group of Object.values(clips)) {
      group?.stop();
    }
    for (const clip of Object.keys(clips) as PlayerClip[]) {
      this.weights.set(clip, clip === "idle" ? 1 : 0);
    }
  }

  update(deltaSeconds: number, input: AnimatorInput): void {
    const target = input.swing ? input.swing.clip : this.locomotionClip(input.groundSpeedMps);
    const targetChanged = target !== this.targetClip;
    this.targetClip = target;
    const snapToReplayTime = input.seeked || (input.timeScale === 0 && targetChanged);
    const step = snapToReplayTime ? 1 : input.timeScale === 0 ? 0 : deltaSeconds / FADE_SECONDS;

    let total = 0;
    for (const clip of this.weights.keys()) {
      const next = approach(this.weights.get(clip) ?? 0, clip === target ? 1 : 0, step);
      this.weights.set(clip, next);
      total += next;
    }
    // Babylon blends an under-weighted pose back toward the bind pose, which
    // shows up as a T-pose ghost mid-fade, so weights must always sum to one.
    if (total <= WEIGHT_EPSILON) {
      this.weights.set(target, 1);
      total = 1;
    }

    for (const [clip, group] of Object.entries(this.clips) as [
      PlayerClip,
      AnimationGroup | undefined,
    ][]) {
      if (!group) continue;
      const weight = (this.weights.get(clip) ?? 0) / total;
      if (weight <= WEIGHT_EPSILON) {
        if (group.isPlaying) group.stop();
        if (this.anchoredSwing === clip) this.anchoredSwing = null;
        continue;
      }

      const anchoring =
        isSwing(clip) &&
        input.swing?.clip === clip &&
        (!group.isPlaying || input.seeked || this.anchoredSwing !== clip);

      if (anchoring && input.swing) {
        // Babylon recomputes the frame from the clip start every tick, so
        // goToFrame cannot hold a pose. Restarting the range at the contact
        // frame is the only way to pin a swing to the moment of impact.
        const from = clamp(
          CONTACT_FRAME[clip] + input.swing.secondsFromContact * CLIP_FPS,
          group.from,
          group.to,
        );
        group.start(false, 1, from, group.to, false);
        // A paused replay can pause the group before Babylon evaluates its
        // first tick, so write the anchored pose once before freezing it.
        group.goToFrame(from);
        this.anchoredSwing = clip;
      } else if (!group.isStarted) {
        group.start(LOOPING.has(clip), 1, group.from, group.to, false);
      }

      group.setWeightForAllAnimatables(weight);
      group.speedRatio = isSwing(clip) ? input.timeScale : this.locomotionRate(clip, input);
    }

    this.setPaused(input.timeScale === 0);
  }

  private locomotionClip(groundSpeedMps: number): PlayerClip {
    return groundSpeedMps >= JOG_THRESHOLD_MPS && this.clips.jog ? "jog" : "idle";
  }

  private locomotionRate(clip: PlayerClip, input: AnimatorInput): number {
    if (clip !== "jog") return 1;
    return clamp(input.groundSpeedMps / JOG_REFERENCE_MPS, JOG_RATE_MIN, JOG_RATE_MAX);
  }

  private setPaused(shouldPause: boolean): void {
    if (shouldPause) {
      // Pause preserves Babylon's current blended pose; speedRatio = 0 alone
      // re-evaluates from the animation range start and visibly jumps backward.
      for (const group of Object.values(this.clips)) {
        if (group?.isPlaying) group.pause();
      }
    } else if (this.paused) {
      for (const [clip, group] of Object.entries(this.clips) as [
        PlayerClip,
        AnimationGroup | undefined,
      ][]) {
        if (group?.isStarted && !group.isPlaying) group.play(LOOPING.has(clip));
      }
    }
    this.paused = shouldPause;
  }

  dispose(): void {
    for (const group of Object.values(this.clips)) {
      group?.stop();
    }
  }
}

function isSwing(clip: PlayerClip): clip is SwingClip {
  return clip !== "idle" && clip !== "jog";
}

function approach(current: number, goal: number, step: number): number {
  if (step >= 1) return goal;
  return current + (goal - current) * Math.max(0, step);
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}
