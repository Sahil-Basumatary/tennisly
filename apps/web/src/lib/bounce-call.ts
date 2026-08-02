import type { ShotSummary } from "@/types/replay";
import { landingIsIn } from "@/lib/court-bounds";

/** Bounce-window call for the active shot, or null before the ball lands. */
export function bounceCallAtTime(
  shot: ShotSummary | null,
  timeSeconds: number,
  shotStartSeconds: number,
): "IN" | "OUT" | null {
  if (!shot) return null;
  const bounceRatio = shot.shotType.includes("SERVE") ? 0.62 : 0.55;
  const bounceAt = shotStartSeconds + shot.flightSeconds * bounceRatio * 0.85;
  if (timeSeconds < bounceAt) return null;
  return landingIsIn(shot.shotType, shot.landing, shot.hitter) ? "IN" : "OUT";
}
