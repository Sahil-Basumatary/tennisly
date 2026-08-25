import { isReplayMatchUuid } from "@/lib/replay-index";
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
  circuitRank: number;
  tournament: string;
  endedAt?: string | null;
  scheduledAt?: string | null;
};

function isLiveStatus(status: UpstreamMatchStatus): boolean {
  return status === "IN_PROGRESS" || status === "SUSPENDED";
}

function compareLive(a: HomeReplayMatch, b: HomeReplayMatch): number {
  const circuit = a.circuitRank - b.circuitRank;
  if (circuit !== 0) return circuit;
  return b.pointsPlayed - a.pointsPlayed;
}

function compareCompleted(a: HomeReplayMatch, b: HomeReplayMatch): number {
  const circuit = a.circuitRank - b.circuitRank;
  if (circuit !== 0) return circuit;
  return (b.endedAt ?? b.scheduledAt ?? "").localeCompare(a.endedAt ?? a.scheduledAt ?? "");
}

export function rankHomeReplayCandidates(
  matches: HomeReplayMatch[],
  fallbackId?: string | null,
): HomeReplayPick[] {
  const live = matches
    .filter(
      (match) => isLiveStatus(match.status) && match.pointsPlayed > 0,
    )
    .sort(compareLive)
    .map((match) => ({ id: match.id, kind: "live" as const }));
  const completed = matches
    .filter(
      (match) => match.status === "COMPLETED" && match.pointsPlayed > 0,
    )
    .sort(compareCompleted)
    .map((match) => ({ id: match.id, kind: "replay" as const }));
  const fallback = fallbackId?.trim() ?? "";
  if (!fallback || !isReplayMatchUuid(fallback)) return [...live, ...completed];
  const known = matches.find((match) => match.id === fallback);
  const fallbackPick = known
    ? { id: known.id, kind: isLiveStatus(known.status) ? ("live" as const) : ("replay" as const) }
    : { id: fallback, kind: "replay" as const };
  const ranked = [...live, ...completed];
  return ranked.some((pick) => pick.id === fallbackPick.id) ? ranked : [...ranked, fallbackPick];
}

export function pickHomeReplayCandidate(
  matches: HomeReplayMatch[],
  fallbackId?: string | null,
): HomeReplayPick | null {
  return rankHomeReplayCandidates(matches, fallbackId)[0] ?? null;
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
