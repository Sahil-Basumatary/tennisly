import { editorialCircuitRank } from "@/lib/tournament-filter";
import type { UpstreamMatch } from "@/types/match-catalogue";
import type { MatchStatus, ScoreCard } from "@/types/scores";

function metadataText(match: UpstreamMatch): string {
  const metadata = match.metadata ?? {};
  return [
    metadata.tournamentName,
    metadata.tournamentShortName,
    metadata.tour,
    metadata.circuit,
    metadata.competition,
  ]
    .filter((value): value is string => typeof value === "string")
    .join(" ");
}

function statusRank(status: MatchStatus): number {
  if (status === "live") return 0;
  if (status === "upcoming") return 1;
  return 2;
}

export function matchCircuitRank(match: UpstreamMatch): number {
  return editorialCircuitRank(metadataText(match));
}

export function compareMatchesByPriority(a: UpstreamMatch, b: UpstreamMatch): number {
  const circuit = matchCircuitRank(a) - matchCircuitRank(b);
  if (circuit !== 0) return circuit;
  const status = statusRankFromUpstream(a.status) - statusRankFromUpstream(b.status);
  if (status !== 0) return status;
  const aTime = a.scheduledAt ?? "";
  const bTime = b.scheduledAt ?? "";
  const time =
    a.status === "SCHEDULED" ? aTime.localeCompare(bTime) : bTime.localeCompare(aTime);
  if (time !== 0) return time;
  return a.id.localeCompare(b.id);
}

export function compareScoreCardsByPriority(a: ScoreCard, b: ScoreCard): number {
  const circuit = a.circuitRank - b.circuitRank;
  if (circuit !== 0) return circuit;
  const status = statusRank(a.status) - statusRank(b.status);
  if (status !== 0) return status;
  return a.id.localeCompare(b.id);
}

function statusRankFromUpstream(status: UpstreamMatch["status"]): number {
  if (status === "IN_PROGRESS" || status === "SUSPENDED") return 0;
  if (status === "SCHEDULED") return 1;
  return 2;
}
