import {
  FRAME_RATE,
  GROUNDSTROKE_CONTACT_HEIGHT_METRES,
  HALF_LENGTH_METRES,
  SERVE_CONTACT_HEIGHT_METRES,
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import { isReplayMatchUuid } from "@/lib/replay-index";
import {
  lerpVector3,
  roundMetres,
  roundVector3,
  shotArcPoint,
} from "@/lib/replay-space";
import type {
  MatchReplay,
  PointSummary,
  ReplayFrame,
  ShotSummary,
  ShotType,
  SpinType,
  Vector3,
} from "@/types/replay";

/** Mock player UUIDs — court-preview stats HUD buckets against these. */
export const MOCK_REPLAY_HOME_PLAYER_ID = "00000000-0000-4000-8000-000000000001";
export const MOCK_REPLAY_AWAY_PLAYER_ID = "00000000-0000-4000-8000-000000000002";
const HOME_ID = MOCK_REPLAY_HOME_PLAYER_ID;
const AWAY_ID = MOCK_REPLAY_AWAY_PLAYER_ID;
const MATCH_ID = "00000000-0000-4000-8000-0000000000aa";

const BASELINE_REST = HALF_LENGTH_METRES - 0.9;

type ShotSpec = {
  shotType: ShotType;
  hitter: "HOME" | "AWAY";
  spin: SpinType;
  contact: Vector3;
  landing: Vector3;
  apexHeightMetres: number;
  flightSeconds: number;
  launchSpeedKmh: number;
  receiverStart: Vector3;
  receiverEnd: Vector3;
};

function v(x: number, y: number, z: number): Vector3 {
  return roundVector3({ x, y, z });
}

/**
 * One grass-court rally: first serve → FH return → BH → FH winner.
 * Numbers sit in the same ranges the Java trajectory engine emits.
 */
function buildSampleShots(): ShotSpec[] {
  const serveContact = v(0.35, -(HALF_LENGTH_METRES - 0.4), SERVE_CONTACT_HEIGHT_METRES);
  const serveLanding = v(-1.1, SERVICE_LINE_FROM_NET_METRES - 1.2, 0);
  const returnContact = v(-2.4, HALF_LENGTH_METRES - 1.1, GROUNDSTROKE_CONTACT_HEIGHT_METRES);
  const returnLanding = v(1.8, -(HALF_LENGTH_METRES - 2.4), 0);
  const bhContact = v(2.1, -(HALF_LENGTH_METRES - 1.6), GROUNDSTROKE_CONTACT_HEIGHT_METRES);
  const bhLanding = v(-3.2, HALF_LENGTH_METRES - 3.0, 0);
  const winnerContact = v(-2.8, HALF_LENGTH_METRES - 2.2, GROUNDSTROKE_CONTACT_HEIGHT_METRES);
  const winnerLanding = v(4.55, -(HALF_LENGTH_METRES - 0.8), 0);

  return [
    {
      shotType: "FIRST_SERVE",
      hitter: "HOME",
      spin: "FLAT",
      contact: serveContact,
      landing: serveLanding,
      apexHeightMetres: 3.1,
      flightSeconds: 0.92,
      launchSpeedKmh: 198,
      receiverStart: v(0, BASELINE_REST, 0),
      receiverEnd: v(returnContact.x, returnContact.y, 0),
    },
    {
      shotType: "FOREHAND_GROUNDSTROKE",
      hitter: "AWAY",
      spin: "TOPSPIN",
      contact: returnContact,
      landing: returnLanding,
      apexHeightMetres: 2.4,
      flightSeconds: 1.18,
      launchSpeedKmh: 118,
      receiverStart: v(serveContact.x, serveContact.y, 0),
      receiverEnd: v(bhContact.x, bhContact.y, 0),
    },
    {
      shotType: "BACKHAND_GROUNDSTROKE",
      hitter: "HOME",
      spin: "TOPSPIN",
      contact: bhContact,
      landing: bhLanding,
      apexHeightMetres: 2.55,
      flightSeconds: 1.05,
      launchSpeedKmh: 132,
      receiverStart: v(returnContact.x, returnContact.y, 0),
      receiverEnd: v(winnerContact.x, winnerContact.y, 0),
    },
    {
      shotType: "FOREHAND_GROUNDSTROKE",
      hitter: "AWAY",
      spin: "TOPSPIN",
      contact: winnerContact,
      landing: winnerLanding,
      apexHeightMetres: 1.85,
      flightSeconds: 0.88,
      launchSpeedKmh: 146,
      receiverStart: v(bhContact.x, bhContact.y, 0),
      receiverEnd: v(winnerLanding.x * 0.4, -(BASELINE_REST - 0.5), 0),
    },
  ];
}

function ballAtProgress(shot: ShotSpec, localT: number): Vector3 {
  const bounceT = shot.shotType.includes("SERVE") ? 0.62 : 0.55;
  if (localT <= bounceT) {
    return shotArcPoint(
      shot.contact,
      shot.landing,
      shot.apexHeightMetres,
      localT / bounceT,
    );
  }
  const after = (localT - bounceT) / (1 - bounceT);
  const nextContact: Vector3 = {
    x: shot.receiverEnd.x,
    y: shot.receiverEnd.y,
    z: GROUNDSTROKE_CONTACT_HEIGHT_METRES,
  };
  const bounceApex = Math.min(1.2, shot.apexHeightMetres * 0.35);
  return shotArcPoint(shot.landing, nextContact, bounceApex, after);
}

function framesForShot(
  shot: ShotSpec,
  shotIndex: number,
  pointSequence: number,
  startTime: number,
): ReplayFrame[] {
  const frameCount = Math.max(1, Math.round(shot.flightSeconds * FRAME_RATE));
  const hitterPos = v(shot.contact.x, shot.contact.y, 0);
  const frames: ReplayFrame[] = [];
  for (let i = 0; i <= frameCount; i++) {
    const localT = Math.min(1, i / frameCount);
    const localTime = localT * shot.flightSeconds;
    const receiver = lerpVector3(shot.receiverStart, shot.receiverEnd, localT);
    const home = shot.hitter === "HOME" ? hitterPos : v(receiver.x, receiver.y, 0);
    const away = shot.hitter === "AWAY" ? hitterPos : v(receiver.x, receiver.y, 0);
    frames.push({
      timeSeconds: roundMetres(startTime + localTime),
      ball: roundVector3(ballAtProgress(shot, localT)),
      home,
      away,
      pointSequence,
      shotIndex,
      shotType: shot.shotType,
    });
  }
  return frames;
}

function buildMatchReplay(): MatchReplay {
  const pointOneShots = buildSampleShots();
  const aceContact = v(0.2, -(HALF_LENGTH_METRES - 0.35), SERVE_CONTACT_HEIGHT_METRES);
  const aceLanding = v(1.4, SERVICE_LINE_FROM_NET_METRES - 0.9, 0);
  const pointTwoShots: ShotSpec[] = [
    {
      shotType: "FIRST_SERVE",
      hitter: "HOME",
      spin: "FLAT",
      contact: aceContact,
      landing: aceLanding,
      apexHeightMetres: 2.9,
      flightSeconds: 0.78,
      launchSpeedKmh: 204,
      receiverStart: v(0.4, BASELINE_REST, 0),
      receiverEnd: v(aceLanding.x, aceLanding.y + 0.6, 0),
    },
  ];

  const summaries: ShotSummary[] = [];
  const frames: ReplayFrame[] = [];
  const points: PointSummary[] = [];
  let cursor = 0;
  let shotIndex = 0;

  const appendPoint = (
    sequence: number,
    specs: ShotSpec[],
    outcome: PointSummary["outcome"],
    winnerId: string,
    serverId: string,
  ) => {
    const pointStart = cursor;
    for (const spec of specs) {
      summaries.push({
        pointSequence: sequence,
        shotIndex,
        shotType: spec.shotType,
        hitter: spec.hitter,
        spin: spec.spin,
        contact: spec.contact,
        landing: spec.landing,
        launchSpeedKmh: spec.launchSpeedKmh,
        apexHeightMetres: spec.apexHeightMetres,
        flightSeconds: spec.flightSeconds,
      });
      const shotFrames = framesForShot(spec, shotIndex, sequence, cursor);
      if (frames.length > 0 && shotFrames.length > 0) {
        frames.push(...shotFrames.slice(1));
      } else {
        frames.push(...shotFrames);
      }
      cursor += spec.flightSeconds;
      shotIndex += 1;
    }
    points.push({
      sequence,
      serverId,
      winnerId,
      outcome,
      rallyLength: specs.length,
      shotCount: specs.length,
      durationSeconds: roundMetres(cursor - pointStart),
    });
  };

  appendPoint(1, pointOneShots, "WINNER", AWAY_ID, HOME_ID);
  // Small pause between points so the transport clock reads like a real cut.
  cursor = roundMetres(cursor + 0.35);
  appendPoint(2, pointTwoShots, "ACE", HOME_ID, HOME_ID);

  return {
    matchId: MATCH_ID,
    surface: "GRASS",
    frameRate: FRAME_RATE,
    pointCount: points.length,
    shotCount: summaries.length,
    frameCount: frames.length,
    durationSeconds: roundMetres(cursor),
    points,
    shots: summaries,
    frames,
  };
}

const CACHED = buildMatchReplay();

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
        outcome: p.outcome as PointSummary["outcome"],
        rallyLength: Number(p.rallyLength),
        shotCount: Number(p.shotCount),
        durationSeconds: Number(p.durationSeconds),
      };
    }),
    shots: shots as ShotSummary[],
    frames: frames as ReplayFrame[],
  };
}

/**
 * Live path: UUID matchId → Next BFF → replay-service.
 * Scaffold ids (m-alcaraz-…) and offline failures keep the authored mock rally.
 */
export async function getMatchReplay(matchId?: string): Promise<MatchReplay> {
  if (matchId && isReplayMatchUuid(matchId)) {
    try {
      const response = await fetch(`/api/replays/matches/${matchId}`, { cache: "no-store" });
      if (response.ok) {
        return normalizeMatchReplay((await response.json()) as Record<string, unknown>);
      }
      if (process.env.NODE_ENV === "development") {
        console.warn("[replay] upstream", response.status, "— using mock");
      }
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.warn("[replay] fetch failed — using mock", err);
      }
    }
  }
  return CACHED;
}

export async function getPointReplay(
  matchId?: string,
  sequence?: number,
): Promise<{
  matchId: string;
  surface: MatchReplay["surface"];
  frameRate: number;
  point: PointSummary;
  shots: ShotSummary[];
  frames: ReplayFrame[];
}> {
  const replay = await getMatchReplay(matchId);
  const point =
    replay.points.find((entry) => entry.sequence === sequence) ?? replay.points[0];
  const shots = replay.shots.filter((shot) => shot.pointSequence === point.sequence);
  const frames = replay.frames.filter((frame) => frame.pointSequence === point.sequence);
  return {
    matchId: replay.matchId,
    surface: replay.surface,
    frameRate: replay.frameRate,
    point,
    shots,
    frames,
  };
}

export function courtBoundsForSurface() {
  return {
    halfLength: HALF_LENGTH_METRES,
    singlesHalfWidth: SINGLES_HALF_WIDTH_METRES,
  };
}
