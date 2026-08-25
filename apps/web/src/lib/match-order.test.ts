import { describe, expect, it } from "vitest";
import { toScoreboardDay, toScoresFeed } from "@/lib/match-mapper";
import { matchCircuitRank } from "@/lib/match-order";
import type { UpstreamMatch, UpstreamMatchStatus } from "@/types/match-catalogue";

function match({
  id,
  tournament,
  status,
  tour,
}: {
  id: string;
  tournament: string;
  status: UpstreamMatchStatus;
  tour?: string;
}): UpstreamMatch {
  return {
    id,
    externalId: `lta-${id}`,
    tournamentId: tournament,
    surface: "HARD",
    status,
    bestOfSets: 3,
    scheduledAt: "2026-08-25T12:00:00Z",
    metadata: { tournamentName: tournament, tour },
    currentScore: {},
    players: [
      {
        id: `${id}-home`,
        playerId: `${id}-home-player`,
        displayName: "Home Player",
        side: "HOME",
      },
      {
        id: `${id}-away`,
        playerId: `${id}-away-player`,
        displayName: "Away Player",
        side: "AWAY",
      },
    ],
    pointsPlayed: 0,
  };
}

describe("match priority", () => {
  it("orders majors, main tours, team events, then regional events", () => {
    const rows = [
      match({ id: "itf", tournament: "W15 Bielsko Biala", status: "IN_PROGRESS" }),
      match({
        id: "wta",
        tournament: "Monterrey Open",
        tour: "wta",
        status: "COMPLETED",
      }),
      match({ id: "davis", tournament: "Davis Cup", status: "IN_PROGRESS" }),
      match({ id: "slam", tournament: "US Open", status: "SCHEDULED" }),
    ];

    expect(toScoresFeed(rows).items.map((row) => row.id)).toEqual([
      "slam",
      "wta",
      "davis",
      "itf",
    ]);
    expect(toScoreboardDay(rows).groups.map((group) => group.tournamentId)).toEqual([
      "US Open",
      "Monterrey Open",
      "Davis Cup",
      "W15 Bielsko Biala",
    ]);
  });

  it("uses stored tour metadata when the tournament name has no WTA or ATP label", () => {
    const monterrey = match({
      id: "wta",
      tournament: "Monterrey Open",
      tour: "wta",
      status: "IN_PROGRESS",
    });
    expect(matchCircuitRank(monterrey)).toBe(1);
  });
});
