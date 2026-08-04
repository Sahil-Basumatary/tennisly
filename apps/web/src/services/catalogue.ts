import {
  toMatchCentrePanel,
  toScoreboardDay,
  toScoresFeed,
  toTournamentBoard,
} from "@/lib/match-mapper";
import {
  fetchUpstreamMatch,
  fetchUpstreamMatches,
  MatchUpstreamError,
} from "@/lib/match-upstream";
import type { MatchCentrePanel, ScoreboardDay, TournamentBoard } from "@/types/scaffolds";
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
    const match = await fetchUpstreamMatch(id);
    if (!match) return null;
    return toMatchCentrePanel(match);
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.warn("[match-centre] match-service unavailable", err);
    }
    return null;
  }
}

export async function getTournamentBoard(): Promise<TournamentBoard> {
  try {
    const matches = await fetchUpstreamMatches();
    return toTournamentBoard(matches);
  } catch {
    return toTournamentBoard([]);
  }
}
