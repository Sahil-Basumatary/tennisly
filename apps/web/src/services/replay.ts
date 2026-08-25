import {
  FRAME_RATE,
  HALF_LENGTH_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import { isReplayMatchUuid } from "@/lib/replay-index";
import type {
  MatchReplay,
  PointReplay,
  PointSummary,
  ReplayFrame,
  ShotSummary,
  Vector3,
} from "@/types/replay";

function asStringId(value: unknown): string {
  return typeof value === "string" ? value : String(value ?? "");
}

function asVector3(value: unknown): Vector3 | null {
  if (!value || typeof value !== "object") return null;
  const v = value as Record<string, unknown>;
  const x = Number(v.x);
  const y = Number(v.y);
  const z = Number(v.z);
  if (![x, y, z].every(Number.isFinite)) return null;
  return { x, y, z };
}

function asFrame(value: unknown): ReplayFrame | null {
  if (!value || typeof value !== "object") return null;
  const raw = value as Record<string, unknown>;
  const ball = asVector3(raw.ball);
  const home = asVector3(raw.home);
  const away = asVector3(raw.away);
  const timeSeconds = Number(raw.timeSeconds);
  const pointSequence = Number(raw.pointSequence);
  const shotIndex = Number(raw.shotIndex);
  if (!ball || !home || !away) return null;
  if (
    !Number.isFinite(timeSeconds) ||
    !Number.isSafeInteger(pointSequence) ||
    !Number.isSafeInteger(shotIndex)
  ) {
    return null;
  }
  return {
    timeSeconds,
    ball,
    home,
    away,
    pointSequence,
    shotIndex,
    shotType: (raw.shotType as ReplayFrame["shotType"]) ?? "FOREHAND_GROUNDSTROKE",
  };
}

function asPointSummary(value: unknown): PointSummary | null {
  if (!value || typeof value !== "object") return null;
  const p = value as Record<string, unknown>;
  const sequence = Number(p.sequence);
  if (!Number.isSafeInteger(sequence) || sequence < 1) return null;
  return {
    sequence,
    serverId: asStringId(p.serverId),
    winnerId: asStringId(p.winnerId),
    outcome: (p.outcome as PointSummary["outcome"]) ?? "UNKNOWN",
    rallyLength:
      p.rallyLength === null || p.rallyLength === undefined ? null : Number(p.rallyLength),
    shotCount: Number(p.shotCount) || 0,
    durationSeconds: Number(p.durationSeconds) || 0,
    scoreSnapshot:
      p.scoreSnapshot && typeof p.scoreSnapshot === "object"
        ? (p.scoreSnapshot as Record<string, unknown>)
        : undefined,
  };
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
      const parsed = asPointSummary(point);
      if (parsed) return parsed;
      const p = point as Record<string, unknown>;
      return {
        sequence: Number(p.sequence),
        serverId: asStringId(p.serverId),
        winnerId: asStringId(p.winnerId),
        outcome: (p.outcome as PointSummary["outcome"]) ?? "UNKNOWN",
        rallyLength:
          p.rallyLength === null || p.rallyLength === undefined ? null : Number(p.rallyLength),
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

export function normalizePointReplay(raw: unknown): PointReplay | null {
  if (!raw || typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  const point = asPointSummary(o.point);
  if (!point) return null;
  const frames = Array.isArray(o.frames)
    ? o.frames.map(asFrame).filter((frame): frame is ReplayFrame => frame !== null)
    : [];
  if (frames.length === 0) return null;
  const shots = Array.isArray(o.shots) ? (o.shots as ShotSummary[]) : [];
  return {
    matchId: asStringId(o.matchId),
    surface: (o.surface as PointReplay["surface"]) ?? "HARD",
    frameRate: Number(o.frameRate) || FRAME_RATE,
    point,
    shots,
    frames,
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

export async function getPointReplay(
  matchId: string,
  sequence: number,
): Promise<PointReplay | null> {
  if (!isReplayMatchUuid(matchId) || !Number.isSafeInteger(sequence) || sequence < 1) {
    return null;
  }
  try {
    const response = await fetch(`/api/replays/matches/${matchId}/points/${sequence}`, {
      cache: "no-store",
    });
    if (!response.ok) return null;
    return normalizePointReplay(await response.json());
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
