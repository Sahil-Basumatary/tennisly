import type { UpstreamMatch, UpstreamMatchStatus } from "@/types/match-catalogue";

export type LiveScoreDocument = {
  id: string;
  status: UpstreamMatchStatus;
  liveSequence: number;
  pointsPlayed: number;
  updatedAt: string | null;
  currentScore: Record<string, unknown>;
};

export type LiveCursorDocument = {
  id: string;
  status: UpstreamMatchStatus;
  liveSequence: number;
  pointsPlayed: number;
};

const TERMINAL: ReadonlySet<UpstreamMatchStatus> = new Set(["COMPLETED", "CANCELLED"]);

export function isTerminalMatchStatus(status: UpstreamMatchStatus): boolean {
  return TERMINAL.has(status);
}

export function toLiveScoreDocument(match: UpstreamMatch): LiveScoreDocument {
  return {
    id: match.id,
    status: match.status,
    liveSequence: Number.isSafeInteger(match.liveSequence) ? Number(match.liveSequence) : 0,
    pointsPlayed: Number.isSafeInteger(match.pointsPlayed) ? match.pointsPlayed : 0,
    updatedAt: match.updatedAt ?? null,
    currentScore:
      match.currentScore && typeof match.currentScore === "object" ? match.currentScore : {},
  };
}

export function toLiveCursorDocument(match: LiveScoreDocument): LiveCursorDocument {
  return {
    id: match.id,
    status: match.status,
    liveSequence: match.liveSequence,
    pointsPlayed: match.pointsPlayed,
  };
}
