import {
  HALF_LENGTH_METRES,
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import type { PlayerSide, Vector3 } from "@/types/replay";

/** Singles playing area including baselines and singles sidelines. */
export function isInSinglesCourt(point: Vector3): boolean {
  return (
    Math.abs(point.x) <= SINGLES_HALF_WIDTH_METRES + 1e-3 &&
    Math.abs(point.y) <= HALF_LENGTH_METRES + 1e-3
  );
}

/**
 * Legal first/second-serve landing: opposite half, inside a service box
 * (centre line to singles sideline, net to service line).
 */
export function isLegalServeLanding(landing: Vector3, hitter: PlayerSide): boolean {
  const towardPositive = hitter === "HOME";
  const inDepth = towardPositive
    ? landing.y > 0 && landing.y <= SERVICE_LINE_FROM_NET_METRES + 1e-3
    : landing.y < 0 && landing.y >= -SERVICE_LINE_FROM_NET_METRES - 1e-3;
  if (!inDepth) return false;
  return Math.abs(landing.x) <= SINGLES_HALF_WIDTH_METRES + 1e-3 && Math.abs(landing.x) > 1e-3;
}

export function landingIsIn(shotType: string, landing: Vector3, hitter: PlayerSide): boolean {
  if (shotType.includes("SERVE")) return isLegalServeLanding(landing, hitter);
  return isInSinglesCourt(landing);
}

/** Service box centre for a serve landing (Babylon xz → replay xy). */
export function serviceBoxCentre(landing: Vector3, hitter: PlayerSide): Vector3 | null {
  if (!isLegalServeLanding(landing, hitter)) return null;
  const depthSign = hitter === "HOME" ? 1 : -1;
  const lateralSign = landing.x >= 0 ? 1 : -1;
  return {
    x: lateralSign * (SINGLES_HALF_WIDTH_METRES / 2),
    y: depthSign * (SERVICE_LINE_FROM_NET_METRES / 2),
    z: 0,
  };
}
