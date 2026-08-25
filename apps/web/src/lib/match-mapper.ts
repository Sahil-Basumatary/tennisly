import { playerCountry, playerShortName, publicPlayerName } from "@/lib/player-directory";
import { aggregateMatchStats, type UpstreamMatchPoint } from "@/lib/match-stats";
import type { UpstreamMatch, UpstreamMatchPlayer } from "@/types/match-catalogue";
import { toUiMatchStatus } from "@/types/match-catalogue";
import type {
  MatchCentrePanel,
  ScoreboardDay,
  StandingRow,
  TournamentBoard,
} from "@/types/scaffolds";
import type { ScoreCard, ScoresFeed } from "@/types/scores";
import type { Surface } from "@/types/replay";
import { surfaceLabel } from "@/lib/tournament-filter";

function metaString(match: UpstreamMatch, key: string, fallback = ""): string {
  const value = match.metadata?.[key];
  return typeof value === "string" && value.length > 0 ? value : fallback;
}

function sideOf(match: UpstreamMatch, side: "HOME" | "AWAY"): UpstreamMatchPlayer {
  const player = match.players.find((entry) => entry.side === side);
  if (!player) {
    throw new Error(`Match ${match.id} missing ${side} player`);
  }
  return player;
}

function asNumberArray(value: unknown): number[] {
  if (!Array.isArray(value)) return [];
  return value.map((entry) => Number(entry)).filter((n) => Number.isFinite(n));
}

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

/** ESPN-style set line: games won in each set, including the current set while live. */
function scoreLine(match: UpstreamMatch, side: "HOME" | "AWAY"): number[] {
  const score = match.currentScore ?? {};
  const fromPairs = gamesFromPairs(score.games, side === "HOME" ? 0 : 1);
  if (fromPairs.length > 0) return fromPairs;
  const setsRoot = score.sets;
  let completed: number[] = [];
  if (Array.isArray(setsRoot) && setsRoot[0] && typeof setsRoot[0] === "object" && !Array.isArray(setsRoot[0])) {
    const bag = setsRoot[0] as Record<string, unknown>;
    completed = asNumberArray(bag[side]);
  }
  const game = (score.game ?? {}) as Record<string, unknown>;
  const currentGames = Number(side === "HOME" ? game.homeGames : game.awayGames);
  const ui = toUiMatchStatus(match.status);
  if (ui === "live" && Number.isFinite(currentGames)) {
    return [...completed, currentGames];
  }
  return completed;
}

function pointLabel(match: UpstreamMatch, side: "HOME" | "AWAY"): string {
  const score = match.currentScore ?? {};
  const game = (score.game ?? {}) as Record<string, unknown>;
  const fromGame = game[side];
  if (typeof fromGame === "string" && fromGame.length > 0) return fromGame;
  const points = score.points;
  if (Array.isArray(points) && points.length >= 2) {
    const raw = points[side === "HOME" ? 0 : 1];
    if (typeof raw === "string" && raw.length > 0) return raw;
    if (typeof raw === "number" && Number.isFinite(raw)) return String(raw);
  }
  return "0";
}

function startLabel(match: UpstreamMatch): string | undefined {
  const ui = toUiMatchStatus(match.status);
  if (ui === "live") return "Live";
  if (ui === "final") return "Final";
  if (!match.scheduledAt) return undefined;
  return new Intl.DateTimeFormat("en-GB", {
    hour: "numeric",
    minute: "2-digit",
    timeZone: "UTC",
    timeZoneName: "short",
  }).format(new Date(match.scheduledAt));
}

function hrefFor(match: UpstreamMatch): string {
  const slug = match.externalId?.trim();
  return `/matches/${slug && slug.length > 0 ? slug : match.id}`;
}

function shortDisplayName(displayName: string): string {
  const name = publicPlayerName(displayName);
  const parts = name.split(/\s+/);
  if (parts.length === 1) return parts[0] ?? name;
  const first = parts[0]?.[0] ?? "";
  return `${first}. ${parts[parts.length - 1]}`;
}

export function toScoreCard(match: UpstreamMatch): ScoreCard {
  const home = sideOf(match, "HOME");
  const away = sideOf(match, "AWAY");
  const status = toUiMatchStatus(match.status);
  const homeSets = scoreLine(match, "HOME");
  const awaySets = scoreLine(match, "AWAY");
  let homeWinner = false;
  let awayWinner = false;
  if (status === "final") {
    let homeSetWins = 0;
    let awaySetWins = 0;
    const len = Math.max(homeSets.length, awaySets.length);
    for (let i = 0; i < len; i++) {
      const h = homeSets[i] ?? 0;
      const a = awaySets[i] ?? 0;
      if (h > a) homeSetWins++;
      if (a > h) awaySetWins++;
    }
    homeWinner = homeSetWins > awaySetWins;
    awayWinner = awaySetWins > homeSetWins;
  }
  return {
    id: match.id,
    status,
    tournament: metaString(match, "tournamentShortName", metaString(match, "tournamentName", "Tour")),
    round: metaString(match, "roundCode", metaString(match, "round", "—")),
    startLabel: startLabel(match),
    href: hrefFor(match),
    home: {
      name: shortDisplayName(home.displayName),
      fullName: publicPlayerName(home.displayName),
      shortName: playerShortName(home.displayName),
      sets: homeSets,
      winner: homeWinner || undefined,
    },
    away: {
      name: shortDisplayName(away.displayName),
      fullName: publicPlayerName(away.displayName),
      shortName: playerShortName(away.displayName),
      sets: awaySets,
      winner: awayWinner || undefined,
    },
  };
}

export function toScoresFeed(matches: UpstreamMatch[]): ScoresFeed {
  const sorted = [...matches].sort((a, b) => {
    const rank = (status: UpstreamMatch["status"]) => {
      if (status === "IN_PROGRESS" || status === "SUSPENDED") return 0;
      if (status === "SCHEDULED") return 1;
      return 2;
    };
    const byStatus = rank(a.status) - rank(b.status);
    if (byStatus !== 0) return byStatus;
    return (a.scheduledAt ?? "").localeCompare(b.scheduledAt ?? "");
  });
  return {
    updatedAt: new Date().toISOString(),
    items: sorted.map(toScoreCard),
  };
}

export function toScoreboardDay(matches: UpstreamMatch[], date = new Date()): ScoreboardDay {
  const feed = toScoresFeed(matches);
  const groupsMap = new Map<string, { tournamentId: string; tournamentName: string; location: string; matches: ScoreCard[] }>();
  for (const match of matches) {
    const card = feed.items.find((item) => item.id === match.id);
    if (!card) continue;
    const tournamentId = match.tournamentId ?? metaString(match, "tournamentShortName", "tour");
    const existing = groupsMap.get(tournamentId);
    if (existing) {
      existing.matches.push(card);
      continue;
    }
    groupsMap.set(tournamentId, {
      tournamentId,
      tournamentName: metaString(match, "tournamentName", card.tournament),
      location: metaString(match, "location", metaString(match, "court", "Centre courts")),
      matches: [card],
    });
  }
  return {
    dateLabel: new Intl.DateTimeFormat("en-GB", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
      timeZone: "UTC",
    }).format(date),
    filters: [
      { id: "all", label: "All matches" },
      { id: "mens_singles", label: "Men's Singles" },
      { id: "womens_singles", label: "Women's Singles" },
      { id: "mens_doubles", label: "Men's Doubles" },
      { id: "womens_doubles", label: "Women's Doubles" },
    ],
    groups: [...groupsMap.values()],
  };
}

export function toMatchCentrePanel(
  match: UpstreamMatch,
  points: UpstreamMatchPoint[] = [],
): MatchCentrePanel {
  const home = sideOf(match, "HOME");
  const away = sideOf(match, "AWAY");
  const status = toUiMatchStatus(match.status);
  const homeSets = scoreLine(match, "HOME");
  const awaySets = scoreLine(match, "AWAY");
  const live = status === "live";
  const homeGames = live ? (homeSets[homeSets.length - 1] ?? 0) : 0;
  const awayGames = live ? (awaySets[awaySets.length - 1] ?? 0) : 0;
  const completedHome = live ? homeSets.slice(0, -1) : homeSets;
  const completedAway = live ? awaySets.slice(0, -1) : awaySets;
  const serverId = String(match.currentScore?.serverId ?? "");
  const server: "HOME" | "AWAY" =
    serverId === away.playerId ? "AWAY" : "HOME";
  const stats =
    points.length > 0
      ? aggregateMatchStats(match, points)
      : [
          { label: "Points played", home: String(match.pointsPlayed), away: String(match.pointsPlayed) },
          { label: "Surface", home: match.surface, away: match.surface },
          { label: "Best of", home: String(match.bestOfSets), away: String(match.bestOfSets) },
        ];
  return {
    id: match.id,
    status,
    tournament: metaString(match, "tournamentName", metaString(match, "tournamentShortName", "Tour")),
    round: metaString(match, "round", metaString(match, "roundCode", "—")),
    court: metaString(match, "court", "Centre Court"),
    surface: match.surface as Surface,
    home: {
      id: home.playerId,
      name: publicPlayerName(home.displayName),
      country: playerCountry(metaString(match, "homeNationality") || undefined),
      seed: home.seedNumber ?? undefined,
    },
    away: {
      id: away.playerId,
      name: publicPlayerName(away.displayName),
      country: playerCountry(metaString(match, "awayNationality") || undefined),
      seed: away.seedNumber ?? undefined,
    },
    score: {
      homeSets: completedHome,
      awaySets: completedAway,
      homeGames,
      awayGames,
      homePoints: live ? pointLabel(match, "HOME") : "0",
      awayPoints: live ? pointLabel(match, "AWAY") : "0",
      server,
    },
    stats,
  };
}

export function toTournamentBoard(
  matches: UpstreamMatch[],
  standings: StandingRow[] = [],
  heading?: { name: string; location: string; standingsLabel?: string },
): TournamentBoard {
  const primary = matches[0];
  const fixtures = toScoresFeed(matches).items.map((card) => ({
    id: card.id,
    status: card.status,
    startLabel: card.startLabel ?? (card.status === "live" ? "Live" : "TBD"),
    round: card.round,
    home: card.home.name,
    away: card.away.name,
    href: card.href,
  }));
  return {
    name: heading?.name
      ?? (primary ? metaString(primary, "tournamentName", "Tour board") : "Tour board"),
    surface: surfaceLabel(primary?.surface),
    location: heading?.location
      ?? (primary ? metaString(primary, "location", "—") : "—"),
    standingsLabel: heading?.standingsLabel ?? "Rankings",
    standings,
    fixtures,
  };
}
