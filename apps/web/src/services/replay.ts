import {
  FRAME_RATE,
  GROUNDSTROKE_CONTACT_HEIGHT_METRES,
  HALF_LENGTH_METRES,
  SERVE_CONTACT_HEIGHT_METRES,
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
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

const HOME_ID = "00000000-0000-4000-8000-000000000001";
const AWAY_ID = "00000000-0000-4000-8000-000000000002";
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
  const shots = buildSampleShots();
  const pointSequence = 1;
  const summaries: ShotSummary[] = shots.map((s, shotIndex) => ({
    pointSequence,
    shotIndex,
    shotType: s.shotType,
    hitter: s.hitter,
    spin: s.spin,
    contact: s.contact,
    landing: s.landing,
    launchSpeedKmh: s.launchSpeedKmh,
    apexHeightMetres: s.apexHeightMetres,
    flightSeconds: s.flightSeconds,
  }));

  const frames: ReplayFrame[] = [];
  let cursor = 0;
  for (let i = 0; i < shots.length; i++) {
    const shotFrames = framesForShot(shots[i], i, pointSequence, cursor);
    // Drop duplicate seam frame when chaining shots (except first)
    if (frames.length > 0 && shotFrames.length > 0) {
      frames.push(...shotFrames.slice(1));
    } else {
      frames.push(...shotFrames);
    }
    cursor += shots[i].flightSeconds;
  }

  const durationSeconds = roundMetres(cursor);
  const point: PointSummary = {
    sequence: pointSequence,
    serverId: HOME_ID,
    winnerId: AWAY_ID,
    outcome: "WINNER",
    rallyLength: shots.length,
    shotCount: shots.length,
    durationSeconds,
  };

  return {
    matchId: MATCH_ID,
    surface: "GRASS",
    frameRate: FRAME_RATE,
    pointCount: 1,
    shotCount: summaries.length,
    frameCount: frames.length,
    durationSeconds,
    points: [point],
    shots: summaries,
    frames,
  };
}

const CACHED = buildMatchReplay();

/** Typed mock shaped like MatchReplayResponse — swap for live fetch in phase4-replay-player. */
export async function getMatchReplay(_matchId?: string): Promise<MatchReplay> {
  return CACHED;
}

export async function getPointReplay(
  _matchId?: string,
  _sequence?: number,
): Promise<{
  matchId: string;
  surface: MatchReplay["surface"];
  frameRate: number;
  point: PointSummary;
  shots: ShotSummary[];
  frames: ReplayFrame[];
}> {
  const replay = await getMatchReplay();
  return {
    matchId: replay.matchId,
    surface: replay.surface,
    frameRate: replay.frameRate,
    point: replay.points[0],
    shots: replay.shots,
    frames: replay.frames,
  };
}

export function courtBoundsForSurface() {
  return {
    halfLength: HALF_LENGTH_METRES,
    singlesHalfWidth: SINGLES_HALF_WIDTH_METRES,
  };
}
