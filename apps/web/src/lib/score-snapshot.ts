export type TapeScore = {
  homeSets: number[];
  awaySets: number[];
  homeGames: number;
  awayGames: number;
  homePoints: string;
  awayPoints: string;
  server: "HOME" | "AWAY";
};

function gamesFromPairs(games: unknown, index: 0 | 1): number[] {
  if (!Array.isArray(games)) return [];
  const values: number[] = [];
  for (const pair of games) {
    if (!Array.isArray(pair) || pair.length < 2) continue;
    const n = Number(pair[index]);
    if (Number.isFinite(n)) values.push(n);
  }
  return values;
}

function pointLabel(score: Record<string, unknown>, side: "HOME" | "AWAY"): string | null {
  const game = (score.game ?? {}) as Record<string, unknown>;
  const fromGame = game[side];
  if (typeof fromGame === "string" && fromGame.length > 0) return fromGame;
  const points = score.points;
  if (!Array.isArray(points) || points.length < 2) return null;
  const raw = points[side === "HOME" ? 0 : 1];
  if (typeof raw === "string" && raw.length > 0) return raw;
  if (typeof raw === "number" && Number.isFinite(raw)) return String(raw);
  return null;
}

function serverFromSnapshot(
  snap: Record<string, unknown>,
  fallback: "HOME" | "AWAY",
  homePlayerId?: string,
  awayPlayerId?: string,
): "HOME" | "AWAY" {
  const serverId = String(snap.serverId ?? "");
  if (awayPlayerId && serverId === awayPlayerId) return "AWAY";
  if (homePlayerId && serverId === homePlayerId) return "HOME";
  if (snap.server === 2 || snap.server === "AWAY") return "AWAY";
  if (snap.server === 1 || snap.server === "HOME") return "HOME";
  return fallback;
}

/** Point ledger snapshot → score bug. Missing fields keep the last trusted board. */
export function scoreFromSnapshot(
  snap: Record<string, unknown> | null | undefined,
  fallback: TapeScore,
  homePlayerId?: string,
  awayPlayerId?: string,
): TapeScore {
  if (!snap || typeof snap !== "object") return fallback;
  const homeLine = gamesFromPairs(snap.games, 0);
  const awayLine = gamesFromPairs(snap.games, 1);
  const homePoints = pointLabel(snap, "HOME");
  const awayPoints = pointLabel(snap, "AWAY");
  if (homeLine.length === 0 && awayLine.length === 0 && homePoints === null && awayPoints === null) {
    return { ...fallback, server: serverFromSnapshot(snap, fallback.server, homePlayerId, awayPlayerId) };
  }
  const hasGames = homeLine.length > 0 || awayLine.length > 0;
  return {
    homeSets: hasGames ? homeLine.slice(0, -1) : fallback.homeSets,
    awaySets: hasGames ? awayLine.slice(0, -1) : fallback.awaySets,
    homeGames: hasGames ? (homeLine[homeLine.length - 1] ?? 0) : fallback.homeGames,
    awayGames: hasGames ? (awayLine[awayLine.length - 1] ?? 0) : fallback.awayGames,
    homePoints: homePoints ?? fallback.homePoints,
    awayPoints: awayPoints ?? fallback.awayPoints,
    server: serverFromSnapshot(snap, fallback.server, homePlayerId, awayPlayerId),
  };
}
