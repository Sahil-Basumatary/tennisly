import type { PointSummary } from "@/types/replay";
import { indexAtOrBefore } from "@/lib/replay-transport";

export type ReplaySideStats = {
  aces: number;
  winners: number;
  unforcedErrors: number;
  pointsWon: number;
};

export type ReplayRunningStats = {
  home: ReplaySideStats;
  away: ReplaySideStats;
  completedCount: number;
  totalCount: number;
};

function emptySide(): ReplaySideStats {
  return { aces: 0, winners: 0, unforcedErrors: 0, pointsWon: 0 };
}

function sideFor(
  playerId: string,
  homePlayerId: string,
  awayPlayerId: string,
  home: ReplaySideStats,
  away: ReplaySideStats,
): ReplaySideStats {
  return playerId === awayPlayerId ? away : home;
}

function opponentId(playerId: string, homePlayerId: string, awayPlayerId: string): string {
  return playerId === homePlayerId ? awayPlayerId : homePlayerId;
}

/**
 * Points fully behind the scrubber (current point excluded until the match clock ends).
 */
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

/**
 * Running box-score for the on-canvas HUD — only points already completed.
 */
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
    const winner = sideFor(point.winnerId, homePlayerId, awayPlayerId, home, away);
    winner.pointsWon += 1;
    const outcome = point.outcome.toUpperCase();
    if (outcome === "ACE") {
      sideFor(point.winnerId, homePlayerId, awayPlayerId, home, away).aces += 1;
      continue;
    }
    if (outcome === "WINNER") {
      sideFor(point.winnerId, homePlayerId, awayPlayerId, home, away).winners += 1;
      continue;
    }
    if (outcome === "UNFORCED_ERROR") {
      sideFor(
        opponentId(point.winnerId, homePlayerId, awayPlayerId),
        homePlayerId,
        awayPlayerId,
        home,
        away,
      ).unforcedErrors += 1;
    }
  }

  return {
    home,
    away,
    completedCount: slice.length,
    totalCount: points.length,
  };
}
