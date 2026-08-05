import type { UpstreamMatch, UpstreamMatchPlayer } from "@/types/match-catalogue";

export type UpstreamMatchPoint = {
  id: string;
  sequenceNumber: number;
  serverId: string;
  winnerId: string;
  outcome: string;
  rallyLength: number | null;
  scoreSnapshot?: Record<string, unknown> | null;
};

type SideBag = { homeId: string; awayId: string };

type SideBucket = {
  pointsWon: number;
  servicePointsWon: number;
  breakPointsWon: number;
};

function emptyBucket(): SideBucket {
  return { pointsWon: 0, servicePointsWon: 0, breakPointsWon: 0 };
}

function sideIds(players: UpstreamMatchPlayer[]): SideBag {
  const home = players.find((player) => player.side === "HOME");
  const away = players.find((player) => player.side === "AWAY");
  if (!home || !away) {
    throw new Error("Match players incomplete for stats");
  }
  return { homeId: home.playerId, awayId: away.playerId };
}

function bucketFor(map: SideBag, playerId: string, home: SideBucket, away: SideBucket): SideBucket {
  return playerId === map.awayId ? away : home;
}

function isServiceBreak(point: UpstreamMatchPoint): boolean {
  if (point.winnerId === point.serverId) return false;
  const pts = point.scoreSnapshot?.points;
  if (!Array.isArray(pts) || pts.length < 2) return false;
  return String(pts[0]) === "0" && String(pts[1]) === "0";
}

/** Box score from the point ledger — only metrics the tape can prove. */
export function aggregateMatchStats(
  match: UpstreamMatch,
  points: UpstreamMatchPoint[],
): { label: string; home: string; away: string }[] {
  const map = sideIds(match.players);
  const home = emptyBucket();
  const away = emptyBucket();

  for (const point of points) {
    bucketFor(map, point.winnerId, home, away).pointsWon += 1;
    if (point.winnerId === point.serverId) {
      bucketFor(map, point.serverId, home, away).servicePointsWon += 1;
    } else if (isServiceBreak(point)) {
      bucketFor(map, point.winnerId, home, away).breakPointsWon += 1;
    }
  }

  return [
    {
      label: "Points won",
      home: String(home.pointsWon),
      away: String(away.pointsWon),
    },
    {
      label: "Service points won",
      home: String(home.servicePointsWon),
      away: String(away.servicePointsWon),
    },
    {
      label: "Breaks",
      home: String(home.breakPointsWon),
      away: String(away.breakPointsWon),
    },
    { label: "Points played", home: String(points.length), away: String(points.length) },
  ];
}
