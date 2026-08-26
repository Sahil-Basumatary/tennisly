import { describe, expect, it } from "vitest";
import {
  isTerminalMatchStatus,
  toLiveCursorDocument,
  toLiveScoreDocument,
} from "@/lib/live-score-document";
import type { UpstreamMatch } from "@/types/match-catalogue";

function match(over: Partial<UpstreamMatch> = {}): UpstreamMatch {
  return {
    id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
    surface: "GRASS",
    status: "IN_PROGRESS",
    bestOfSets: 3,
    currentScore: { points: ["30", "15"] },
    players: [],
    pointsPlayed: 12,
    liveSequence: 18,
    updatedAt: "2026-08-26T12:00:00Z",
    ...over,
  };
}

describe("live score document", () => {
  it("keeps only score and cursor fields", () => {
    const live = toLiveScoreDocument(
      match({
        metadata: { tournamentName: "Wimbledon" },
        players: [
          {
            id: "h",
            playerId: "hp",
            displayName: "Home",
            side: "HOME",
          },
        ],
      }),
    );
    expect(live).toEqual({
      id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
      status: "IN_PROGRESS",
      liveSequence: 18,
      pointsPlayed: 12,
      updatedAt: "2026-08-26T12:00:00Z",
      currentScore: { points: ["30", "15"] },
    });
    expect(toLiveCursorDocument(live)).toEqual({
      id: live.id,
      status: "IN_PROGRESS",
      liveSequence: 18,
      pointsPlayed: 12,
    });
  });

  it("treats completed matches as immutable cache candidates", () => {
    expect(isTerminalMatchStatus("COMPLETED")).toBe(true);
    expect(isTerminalMatchStatus("CANCELLED")).toBe(true);
    expect(isTerminalMatchStatus("IN_PROGRESS")).toBe(false);
  });
});
