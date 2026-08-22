import type { MatchStatus as UiMatchStatus } from "@/types/scores";

export type UpstreamMatchStatus =
  | "SCHEDULED"
  | "IN_PROGRESS"
  | "SUSPENDED"
  | "COMPLETED"
  | "CANCELLED";

export type UpstreamSurface = "HARD" | "CLAY" | "GRASS";

export type UpstreamMatchPlayer = {
  id: string;
  playerId: string;
  displayName: string;
  side: "HOME" | "AWAY";
  seedNumber?: number | null;
};

export type UpstreamMatch = {
  id: string;
  externalId?: string | null;
  tournamentId?: string | null;
  surface: UpstreamSurface;
  status: UpstreamMatchStatus;
  bestOfSets: number;
  scheduledAt?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
  metadata?: Record<string, unknown>;
  currentScore?: Record<string, unknown>;
  players: UpstreamMatchPlayer[];
  pointsPlayed: number;
  liveSequence?: number;
};

export function toUiMatchStatus(status: UpstreamMatchStatus): UiMatchStatus {
  if (status === "IN_PROGRESS" || status === "SUSPENDED") return "live";
  if (status === "COMPLETED" || status === "CANCELLED") return "final";
  return "upcoming";
}

export function toUpstreamStatus(ui?: string): UpstreamMatchStatus | undefined {
  if (ui === "live") return "IN_PROGRESS";
  if (ui === "upcoming") return "SCHEDULED";
  if (ui === "final") return "COMPLETED";
  return undefined;
}
