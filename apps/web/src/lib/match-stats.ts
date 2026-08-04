import type { UpstreamMatch, UpstreamMatchPlayer } from "@/types/match-catalogue";

export type UpstreamMatchPoint = {
  id: string;
  sequenceNumber: number;
  serverId: string;
  winnerId: string;
  outcome: string;
  rallyLength: number;
};

type SideBag = { homeId: string; awayId: string };

type SideBucket = {
  aces: number;
  winners: number;
  unforcedErrors: number;
  forcedErrors: number;
  doubleFaults: number;
  pointsWon: number;
};

function emptyBucket(): SideBucket {
  return {
    aces: 0,
    winners: 0,
    unforcedErrors: 0,
    forcedErrors: 0,
    doubleFaults: 0,
    pointsWon: 0,
  };
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

function opponent(map: SideBag, playerId: string): string {
  return playerId === map.homeId ? map.awayId : map.homeId;
}

/**
 * ESPN/UEFA-style box score from the point ledger (aces, winners, errors, DF, avg rally).
 */
export function aggregateMatchStats(
  match: UpstreamMatch,
  points: UpstreamMatchPoint[],
): { label: string; home: string; away: string }[] {
  const map = sideIds(match.players);
  const home = emptyBucket();
  const away = emptyBucket();

  for (const point of points) {
    const winner = bucketFor(map, point.winnerId, home, away);
    winner.pointsWon += 1;
    const outcome = point.outcome.toUpperCase();
    if (outcome === "ACE") {
      bucketFor(map, point.winnerId, home, away).aces += 1;
      continue;
    }
    if (outcome === "WINNER") {
      bucketFor(map, point.winnerId, home, away).winners += 1;
      continue;
    }
    if (outcome === "DOUBLE_FAULT") {
      bucketFor(map, point.serverId, home, away).doubleFaults += 1;
      continue;
    }
    if (outcome === "UNFORCED_ERROR") {
      bucketFor(map, opponent(map, point.winnerId), home, away).unforcedErrors += 1;
      continue;
    }
    if (outcome === "FORCED_ERROR") {
      bucketFor(map, opponent(map, point.winnerId), home, away).forcedErrors += 1;
    }
  }

  const rallyPoints = points.filter((point) => point.rallyLength > 0);
  const avgRally =
    rallyPoints.length === 0
      ? "—"
      : (
          rallyPoints.reduce((sum, point) => sum + point.rallyLength, 0) / rallyPoints.length
        ).toFixed(1);

  return [
    { label: "Aces", home: String(home.aces), away: String(away.aces) },
    { label: "Winners", home: String(home.winners), away: String(away.winners) },
    {
      label: "Unforced errors",
      home: String(home.unforcedErrors),
      away: String(away.unforcedErrors),
    },
    {
      label: "Forced errors",
      home: String(home.forcedErrors),
      away: String(away.forcedErrors),
    },
    {
      label: "Double faults",
      home: String(home.doubleFaults),
      away: String(away.doubleFaults),
    },
    {
      label: "Points won",
      home: String(home.pointsWon),
      away: String(away.pointsWon),
    },
    { label: "Avg rally length", home: avgRally, away: avgRally },
    { label: "Points played", home: String(points.length), away: String(points.length) },
  ];
}
