export type Surface = "HARD" | "CLAY" | "GRASS";

export type ShotType =
  | "FIRST_SERVE"
  | "SECOND_SERVE"
  | "FOREHAND_GROUNDSTROKE"
  | "BACKHAND_GROUNDSTROKE"
  | "FOREHAND_VOLLEY"
  | "BACKHAND_VOLLEY"
  | "FOREHAND_SLICE"
  | "BACKHAND_SLICE"
  | "DROP_SHOT"
  | "LOB"
  | "OVERHEAD";

export type SpinType = "TOPSPIN" | "BACKSPIN" | "FLAT";

export type PlayerSide = "HOME" | "AWAY";

export type PointOutcome =
  | "WINNER"
  | "FORCED_ERROR"
  | "UNFORCED_ERROR"
  | "ACE"
  | "DOUBLE_FAULT"
  | "UNKNOWN";

/** Court frame: x lateral, y depth (net→baseline), z height — metres. */
export type Vector3 = {
  x: number;
  y: number;
  z: number;
};

export type ReplayFrame = {
  timeSeconds: number;
  ball: Vector3;
  home: Vector3;
  away: Vector3;
  pointSequence: number;
  shotIndex: number;
  shotType: ShotType;
};

export type ShotSummary = {
  pointSequence: number;
  shotIndex: number;
  shotType: ShotType;
  hitter: PlayerSide;
  spin: SpinType;
  contact: Vector3;
  landing: Vector3;
  launchSpeedKmh: number;
  apexHeightMetres: number;
  flightSeconds: number;
};

export type PointSummary = {
  sequence: number;
  serverId: string;
  winnerId: string;
  outcome: PointOutcome;
  rallyLength: number | null;
  shotCount: number;
  durationSeconds: number;
  scoreSnapshot?: Record<string, unknown>;
};

export type MatchReplay = {
  matchId: string;
  surface: Surface;
  frameRate: number;
  pointCount: number;
  shotCount: number;
  frameCount: number;
  durationSeconds: number;
  points: PointSummary[];
  shots: ShotSummary[];
  frames: ReplayFrame[];
};

export type PointReplay = {
  matchId: string;
  surface: Surface;
  frameRate: number;
  point: PointSummary;
  shots: ShotSummary[];
  frames: ReplayFrame[];
};
