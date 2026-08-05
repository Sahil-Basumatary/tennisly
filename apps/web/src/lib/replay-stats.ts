import type { PointSummary } from "@/types/replay";
import { indexAtOrBefore } from "@/lib/replay-transport";

export type ReplaySideStats = {
  pointsWon: number;
  gamesWon: number;
  servicePointsWon: number;
  breakPointsWon: number;
};

export type ReplayRunningStats = {
  home: ReplaySideStats;
  away: ReplaySideStats;
  completedCount: number;
  totalCount: number;
};

function emptySide(): ReplaySideStats {
  return { pointsWon: 0, gamesWon: 0, servicePointsWon: 0, breakPointsWon: 0 };
}

export function completedPointCount(
  pointStartTimes: number[],
  timeSeconds: number,
  durationSeconds = 0,
): number {
  if (pointStartTimes.length === 0) return 0;
  if (durationSeconds > 0 && timeSeconds + 1e-3 >= durationSeconds) {
    return pointStartTimes.length;
  }
  return indexAtOrBefore(pointStartTimes, timeSeconds);
}

/** Running box-score from the point tape — only metrics the ledger can prove. */
export function aggregateReplayStats(
  points: PointSummary[],
  homePlayerId: string,
  awayPlayerId: string,
  completedCount: number,
): ReplayRunningStats {
  const home = emptySide();
  const away = emptySide();
  const slice = points.slice(0, Math.max(0, Math.min(completedCount, points.length)));

  for (const point of slice) {
    const winnerIsAway = point.winnerId === awayPlayerId;
    const winner = winnerIsAway ? away : home;
    winner.pointsWon += 1;
    if (point.winnerId === point.serverId) {
      if (point.serverId === homePlayerId) home.servicePointsWon += 1;
      else away.servicePointsWon += 1;
    } else if (isServiceBreak(point)) {
      if (winnerIsAway) away.breakPointsWon += 1;
      else home.breakPointsWon += 1;
    }
    const games = totalGamesFromSnapshot(point.scoreSnapshot);
    if (games) {
      home.gamesWon = Math.max(home.gamesWon, games.home);
      away.gamesWon = Math.max(away.gamesWon, games.away);
    }
  }

  return {
    home,
    away,
    completedCount: slice.length,
    totalCount: points.length,
  };
}

function isServiceBreak(point: PointSummary): boolean {
  if (point.winnerId === point.serverId) return false;
  const snap = point.scoreSnapshot;
  if (!snap) return false;
  const pts = snap.points;
  if (!Array.isArray(pts) || pts.length < 2) return false;
  return String(pts[0]) === "0" && String(pts[1]) === "0";
}

function totalGamesFromSnapshot(
  snap?: Record<string, unknown>,
): { home: number; away: number } | null {
  if (!snap) return null;
  const games = snap.games;
  if (!Array.isArray(games) || games.length === 0) return null;
  let home = 0;
  let away = 0;
  for (const set of games) {
    if (!Array.isArray(set) || set.length < 2) continue;
    home += Number(set[0]) || 0;
    away += Number(set[1]) || 0;
  }
  return { home, away };
}
