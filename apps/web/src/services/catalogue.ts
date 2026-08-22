import type { UpstreamMatchPoint } from "@/lib/match-stats";
import { isReplayMatchUuid } from "@/lib/replay-index";
import {
  toMatchCentrePanel,
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
  fetchUpstreamPlayers,
  fetchUpstreamRankings,
  TennisDataUpstreamError,
  type UpstreamGender,
} from "@/lib/tennis-data-upstream";
import type {
  MatchCentrePanel,
  PlayersBoard,
  ScoreboardDay,
  TournamentBoard,
} from "@/types/scaffolds";
import type { ScoresFeed } from "@/types/scores";
import { toUpstreamStatus } from "@/types/match-catalogue";

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
    return toScoresFeed(matches);
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
    return toScoreboardDay(matches);
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
      return toMatchCentrePanel(match, points);
    }
    const match = await fetchUpstreamMatch(id);
    if (!match) return null;
    const points = await fetchUpstreamMatchPoints(match.id).catch((err) => {
      if (process.env.NODE_ENV === "development") {
        console.warn("[match-centre] points unavailable", err);
      }
      return [] as UpstreamMatchPoint[];
    });
    return toMatchCentrePanel(match, points);
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.warn("[match-centre] match-service unavailable", err);
    }
    return null;
  }
}

export async function getTournamentBoard(): Promise<TournamentBoard> {
  try {
    const [matches, rankings] = await Promise.all([
      fetchUpstreamMatches(),
      fetchUpstreamRankings({ gender: "MALE" }).catch(() => []),
    ]);
    return toTournamentBoard(matches, toStandingRows(rankings, 8));
  } catch {
    return toTournamentBoard([]);
  }
}

export async function getPlayersBoard(tour: "atp" | "wta" = "atp"): Promise<PlayersBoard> {
  const gender: UpstreamGender = tour === "wta" ? "FEMALE" : "MALE";
  try {
    const [rankings, players] = await Promise.all([
      fetchUpstreamRankings({ gender }),
      fetchUpstreamPlayers({ gender }),
    ]);
    return toPlayersBoard(rankings, players, tour);
  } catch (err) {
    if (err instanceof TennisDataUpstreamError && process.env.NODE_ENV === "development") {
      console.warn("[players] tennis-data-service unavailable", err);
    }
    return { tour, updatedAt: new Date().toISOString(), rows: [] };
  }
}
