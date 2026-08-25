import { isReplayMatchUuid } from "@/lib/replay-index";
import { editorialCircuitRank } from "@/lib/tournament-filter";
import type { UpstreamMatchStatus } from "@/types/match-catalogue";
import type { MatchCentrePanel } from "@/types/scaffolds";
import type { MatchStatus } from "@/types/scores";
import type { Surface } from "@/types/replay";

export type HomeReplayKind = "live" | "replay";

export type HomeReplayPick = {
  id: string;
  kind: HomeReplayKind;
};

export type HomeReplayMatch = {
  id: string;
  status: UpstreamMatchStatus;
  pointsPlayed: number;
  tournament: string;
  endedAt?: string | null;
  scheduledAt?: string | null;
};

function isLiveStatus(status: UpstreamMatchStatus): boolean {
  return status === "IN_PROGRESS" || status === "SUSPENDED";
}

function compareLive(a: HomeReplayMatch, b: HomeReplayMatch): number {
  const circuit = editorialCircuitRank(a.tournament) - editorialCircuitRank(b.tournament);
  if (circuit !== 0) return circuit;
  return b.pointsPlayed - a.pointsPlayed;
}

function compareCompleted(a: HomeReplayMatch, b: HomeReplayMatch): number {
  const circuit = editorialCircuitRank(a.tournament) - editorialCircuitRank(b.tournament);
  if (circuit !== 0) return circuit;
  return (b.endedAt ?? b.scheduledAt ?? "").localeCompare(a.endedAt ?? a.scheduledAt ?? "");
}

/**
 * Homepage source: major live with a ledger, any live with a ledger,
 * latest completed major, then a configured known-good UUID.
 */
export function pickHomeReplayCandidate(
  matches: HomeReplayMatch[],
  fallbackId?: string | null,
): HomeReplayPick | null {
  const liveWithPoints = matches.filter((match) => isLiveStatus(match.status) && match.pointsPlayed > 0);
  const majorLive = liveWithPoints
    .filter((match) => editorialCircuitRank(match.tournament) <= 1)
    .sort(compareLive);
  if (majorLive[0]) return { id: majorLive[0].id, kind: "live" };
  const anyLive = [...liveWithPoints].sort(compareLive);
  if (anyLive[0]) return { id: anyLive[0].id, kind: "live" };
  const completed = matches
    .filter((match) => match.status === "COMPLETED" && match.pointsPlayed > 0)
    .sort(compareCompleted);
  if (completed[0]) return { id: completed[0].id, kind: "replay" };
  const fallback = fallbackId?.trim() ?? "";
  if (!fallback || !isReplayMatchUuid(fallback)) return null;
  const known = matches.find((match) => match.id === fallback);
  if (known) {
    return { id: known.id, kind: isLiveStatus(known.status) ? "live" : "replay" };
  }
  return { id: fallback, kind: "replay" };
}

export type HomeReplayFeature = {
  matchId: string;
  href: string;
  kind: HomeReplayKind;
  homeName: string;
  awayName: string;
  homePhotoUrl?: string | null;
  awayPhotoUrl?: string | null;
  homePlayerId: string;
  awayPlayerId: string;
  tournament: string;
  round: string;
  surface: Surface;
  status: MatchStatus;
  score: MatchCentrePanel["score"];
};
