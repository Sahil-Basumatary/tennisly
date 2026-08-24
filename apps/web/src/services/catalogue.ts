import type { UpstreamMatchPoint } from "@/lib/match-stats";
import { isReplayMatchUuid } from "@/lib/replay-index";
import {
  toMatchCentrePanel,
  toScoreCard,
  toScoreboardDay,
  toScoresFeed,
  toTournamentBoard,
} from "@/lib/match-mapper";
import {
  fetchUpstreamMatch,
  fetchUpstreamMatchPoints,
  fetchUpstreamMatches,
  MatchUpstreamError,
} from "@/lib/match-upstream";
import { toPlayersBoard, toStandingRows } from "@/lib/rankings-mapper";
import {
  withMatchCentreHeadshots,
  withPlayerProfileHeadshots,
  withScoreboardHeadshots,
  withScoresFeedHeadshots,
  withTournamentHeadshots,
} from "@/lib/player-photos";
import {
  fetchUpstreamPlayer,
  fetchUpstreamPlayerRankings,
  fetchUpstreamRankings,
  TennisDataUpstreamError,
  type UpstreamGender,
} from "@/lib/tennis-data-upstream";
import type {
  MatchCentrePanel,
  PlayerProfileResult,
  PlayersBoard,
  ScoreboardDay,
  TournamentBoard,
} from "@/types/scaffolds";
import type { ScoresFeed } from "@/types/scores";
import { toUpstreamStatus } from "@/types/match-catalogue";
import {
  filterMatchesForTournament,
  standingsGender,
  tournamentHeading,
  type TournamentQuery,
} from "@/lib/tournament-filter";

/**
 * Live scores strip / live centre feed from match-service.
 * Empty feed when upstream is down — chrome stays up; pages can show an empty board.
 */
export async function getScoresFeed(uiStatus?: string): Promise<ScoresFeed> {
  try {
    const matches = await fetchUpstreamMatches({
      status: toUpstreamStatus(uiStatus),
      page: 0,
      size: 50,
    });
    return withScoresFeedHeadshots(toScoresFeed(matches));
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.warn("[scores] match-service unavailable", err);
    }
    return { updatedAt: new Date().toISOString(), items: [] };
  }
}

export async function getScoreboardDay(uiStatus?: string): Promise<ScoreboardDay> {
  try {
    const matches = await fetchUpstreamMatches({
      status: toUpstreamStatus(uiStatus),
      page: 0,
      size: 50,
    });
    return withScoreboardHeadshots(toScoreboardDay(matches));
  } catch (err) {
    if (err instanceof MatchUpstreamError && process.env.NODE_ENV === "development") {
      console.warn("[scoreboard] match-service unavailable", err);
    }
    return toScoreboardDay([]);
  }
}

export async function getMatchCentre(id: string): Promise<MatchCentrePanel | null> {
  try {
    if (isReplayMatchUuid(id)) {
      const [match, points] = await Promise.all([
        fetchUpstreamMatch(id),
        fetchUpstreamMatchPoints(id).catch((err) => {
          if (process.env.NODE_ENV === "development") {
            console.warn("[match-centre] points unavailable", err);
          }
          return [] as UpstreamMatchPoint[];
        }),
      ]);
      if (!match) return null;
      return withMatchCentreHeadshots(toMatchCentrePanel(match, points));
    }
    const match = await fetchUpstreamMatch(id);
    if (!match) return null;
    const points = await fetchUpstreamMatchPoints(match.id).catch((err) => {
      if (process.env.NODE_ENV === "development") {
        console.warn("[match-centre] points unavailable", err);
      }
      return [] as UpstreamMatchPoint[];
    });
    return withMatchCentreHeadshots(toMatchCentrePanel(match, points));
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.warn("[match-centre] match-service unavailable", err);
    }
    return null;
  }
}

export async function getTournamentBoard(
  query: TournamentQuery = {},
): Promise<TournamentBoard> {
  const heading = tournamentHeading(query);
  const gender = standingsGender(query);
  try {
    const [matches, rankings] = await Promise.all([
      fetchUpstreamMatches({ page: 0, size: 100 }),
      fetchUpstreamRankings({ gender }).catch(() => []),
    ]);
    const filtered = filterMatchesForTournament(matches, query);
    return withTournamentHeadshots(
      toTournamentBoard(filtered, toStandingRows(rankings, 8), heading),
    );
  } catch {
    return withTournamentHeadshots(toTournamentBoard([], [], heading));
  }
}

export async function getPlayerProfile(id: string): Promise<PlayerProfileResult> {
  try {
    const player = await fetchUpstreamPlayer(id);
    if (!player) return { status: "missing" };
    const gender: UpstreamGender = player.gender === "FEMALE" ? "FEMALE" : "MALE";
    const [rankings, matches] = await Promise.all([
      fetchUpstreamPlayerRankings(id).catch(() => []),
      fetchUpstreamMatches({ page: 0, size: 100 }).catch(() => []),
    ]);
    const latest = [...rankings].sort((a, b) => a.rank - b.rank)[0];
    const name = `${player.firstName} ${player.lastName}`.trim();
    return {
      status: "ok",
      player: await withPlayerProfileHeadshots({
        id: player.id,
        name: name || player.lastName,
        country: player.nationality?.trim() || "—",
        tour: gender === "FEMALE" ? "wta" : "atp",
        rank: player.currentRanking ?? latest?.rank ?? null,
        points: player.currentPoints ?? latest?.points ?? null,
        gender: player.gender,
        matches: matches
          .filter((match) => match.players.some((entry) => entry.playerId === id))
          .map(toScoreCard),
      }),
    };
  } catch (err) {
    if (err instanceof TennisDataUpstreamError && err.status === 404) {
      return { status: "missing" };
    }
    if (process.env.NODE_ENV === "development") {
      console.warn("[player] tennis-data-service unavailable", err);
    }
    return { status: "unavailable" };
  }
}

export async function getPlayersBoard(tour: "atp" | "wta" = "atp"): Promise<PlayersBoard> {
  const gender: UpstreamGender = tour === "wta" ? "FEMALE" : "MALE";
  try {
    const rankings = await fetchUpstreamRankings({ gender });
    return toPlayersBoard(rankings, tour);
  } catch (err) {
    if (err instanceof TennisDataUpstreamError && process.env.NODE_ENV === "development") {
      console.warn("[players] tennis-data-service unavailable", err);
    }
    return { tour, updatedAt: new Date().toISOString(), rows: [] };
  }
}
