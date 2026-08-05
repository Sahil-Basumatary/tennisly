import {
  FRAME_RATE,
  HALF_LENGTH_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import { isReplayMatchUuid } from "@/lib/replay-index";
import type {
  MatchReplay,
  PointSummary,
  ReplayFrame,
  ShotSummary,
} from "@/types/replay";

function asStringId(value: unknown): string {
  return typeof value === "string" ? value : String(value ?? "");
}

/** Jackson emits UUID fields as strings; keep the web types stringly typed. */
export function normalizeMatchReplay(raw: Record<string, unknown>): MatchReplay {
  const points = Array.isArray(raw.points) ? raw.points : [];
  const shots = Array.isArray(raw.shots) ? raw.shots : [];
  const frames = Array.isArray(raw.frames) ? raw.frames : [];
  return {
    matchId: asStringId(raw.matchId),
    surface: (raw.surface as MatchReplay["surface"]) ?? "HARD",
    frameRate: Number(raw.frameRate) || FRAME_RATE,
    pointCount: Number(raw.pointCount) || points.length,
    shotCount: Number(raw.shotCount) || shots.length,
    frameCount: Number(raw.frameCount) || frames.length,
    durationSeconds: Number(raw.durationSeconds) || 0,
    points: points.map((point) => {
      const p = point as Record<string, unknown>;
      return {
        sequence: Number(p.sequence),
        serverId: asStringId(p.serverId),
        winnerId: asStringId(p.winnerId),
        outcome: (p.outcome as PointSummary["outcome"]) ?? "UNKNOWN",
        rallyLength:
          p.rallyLength === null || p.rallyLength === undefined
            ? null
            : Number(p.rallyLength),
        shotCount: Number(p.shotCount),
        durationSeconds: Number(p.durationSeconds),
        scoreSnapshot:
          p.scoreSnapshot && typeof p.scoreSnapshot === "object"
            ? (p.scoreSnapshot as Record<string, unknown>)
            : undefined,
      };
    }),
    shots: shots as ShotSummary[],
    frames: frames as ReplayFrame[],
  };
}

/**
 * UUID matchId → Next BFF → replay-service. Returns null when unavailable — never invents a rally.
 */
export async function getMatchReplay(matchId?: string): Promise<MatchReplay | null> {
  if (!matchId || !isReplayMatchUuid(matchId)) {
    return null;
  }
  try {
    const response = await fetch(`/api/replays/matches/${matchId}`, { cache: "no-store" });
    if (!response.ok) {
      return null;
    }
    return normalizeMatchReplay((await response.json()) as Record<string, unknown>);
  } catch {
    return null;
  }
}

export function courtBoundsForSurface() {
  return {
    halfLength: HALF_LENGTH_METRES,
    singlesHalfWidth: SINGLES_HALF_WIDTH_METRES,
  };
}
