import type { ReplayFrame, Vector3 } from "@/types/replay";

/**
 * Replay court frame → Babylon world.
 * Replay: x lateral, y depth, z height.
 * Babylon: x lateral, y height, z depth (Y-up).
 */
export type BabylonVec3 = {
  x: number;
  y: number;
  z: number;
};

export function toBabylon(v: Vector3): BabylonVec3 {
  return { x: v.x, y: v.z, z: v.y };
}

export function fromBabylon(v: BabylonVec3): Vector3 {
  return { x: v.x, y: v.z, z: v.y };
}

export function lerpScalar(a: number, b: number, t: number): number {
  const clamped = Math.max(0, Math.min(1, t));
  return a + (b - a) * clamped;
}

export function lerpVector3(a: Vector3, b: Vector3, t: number): Vector3 {
  return {
    x: lerpScalar(a.x, b.x, t),
    y: lerpScalar(a.y, b.y, t),
    z: lerpScalar(a.z, b.z, t),
  };
}

export function roundMetres(value: number): number {
  return Math.round(value * 1000) / 1000;
}

export function roundVector3(v: Vector3): Vector3 {
  return {
    x: roundMetres(v.x),
    y: roundMetres(v.y),
    z: roundMetres(v.z),
  };
}

/**
 * Quadratic Bezier through contact → apex (mid-depth, apex height) → landing.
 * Used for shot-path overlays when we only have ShotSummary, not dense samples.
 */
export function shotArcPoint(
  contact: Vector3,
  landing: Vector3,
  apexHeightMetres: number,
  t: number,
): Vector3 {
  const mid: Vector3 = {
    x: (contact.x + landing.x) / 2,
    y: (contact.y + landing.y) / 2,
    z: apexHeightMetres,
  };
  const u = 1 - t;
  return {
    x: u * u * contact.x + 2 * u * t * mid.x + t * t * landing.x,
    y: u * u * contact.y + 2 * u * t * mid.y + t * t * landing.y,
    z: u * u * contact.z + 2 * u * t * mid.z + t * t * landing.z,
  };
}

export type InterpolatedPose = {
  timeSeconds: number;
  ball: Vector3;
  home: Vector3;
  away: Vector3;
  pointSequence: number;
  shotIndex: number;
  shotType: ReplayFrame["shotType"];
  frameIndex: number;
};

/**
 * Binary-search the frame list and lerp between neighbours.
 * Frames are assumed sorted ascending by timeSeconds.
 */
export function interpolateAtTime(
  frames: ReplayFrame[],
  timeSeconds: number,
): InterpolatedPose | null {
  if (frames.length === 0) return null;
  if (timeSeconds <= frames[0].timeSeconds) {
    const f = frames[0];
    return {
      timeSeconds: f.timeSeconds,
      ball: f.ball,
      home: f.home,
      away: f.away,
      pointSequence: f.pointSequence,
      shotIndex: f.shotIndex,
      shotType: f.shotType,
      frameIndex: 0,
    };
  }
  const last = frames[frames.length - 1];
  if (timeSeconds >= last.timeSeconds) {
    return {
      timeSeconds: last.timeSeconds,
      ball: last.ball,
      home: last.home,
      away: last.away,
      pointSequence: last.pointSequence,
      shotIndex: last.shotIndex,
      shotType: last.shotType,
      frameIndex: frames.length - 1,
    };
  }

  let lo = 0;
  let hi = frames.length - 1;
  while (lo < hi - 1) {
    const mid = (lo + hi) >> 1;
    if (frames[mid].timeSeconds <= timeSeconds) lo = mid;
    else hi = mid;
  }

  const a = frames[lo];
  const b = frames[hi];
  const span = b.timeSeconds - a.timeSeconds;
  const t = span <= 0 ? 0 : (timeSeconds - a.timeSeconds) / span;

  return {
    timeSeconds,
    ball: lerpVector3(a.ball, b.ball, t),
    home: lerpVector3(a.home, b.home, t),
    away: lerpVector3(a.away, b.away, t),
    pointSequence: t < 0.5 ? a.pointSequence : b.pointSequence,
    shotIndex: t < 0.5 ? a.shotIndex : b.shotIndex,
    shotType: t < 0.5 ? a.shotType : b.shotType,
    frameIndex: lo,
  };
}
