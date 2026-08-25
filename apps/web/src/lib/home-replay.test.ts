import { describe, expect, it } from "vitest";
import {
  pickHomeReplayCandidate,
  rankHomeReplayCandidates,
  type HomeReplayMatch,
} from "@/lib/home-replay";
import { editorialCircuitRank } from "@/lib/tournament-filter";

const fallback = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

function match(over: Partial<HomeReplayMatch> & Pick<HomeReplayMatch, "id" | "status">): HomeReplayMatch {
  const tournament = over.tournament ?? "ITF M15";
  return {
    pointsPlayed: 0,
    circuitRank: editorialCircuitRank(tournament),
    tournament,
    ...over,
  };
}

describe("pickHomeReplayCandidate", () => {
  it("prefers a live slam with points over other live matches", () => {
    const picked = pickHomeReplayCandidate([
      match({
        id: "11111111-1111-4111-8111-111111111111",
        status: "IN_PROGRESS",
        pointsPlayed: 12,
        tournament: "ATP 250 Doha",
      }),
      match({
        id: "22222222-2222-4222-8222-222222222222",
        status: "IN_PROGRESS",
        pointsPlayed: 4,
        tournament: "Wimbledon",
      }),
      match({
        id: "33333333-3333-4333-8333-333333333333",
        status: "IN_PROGRESS",
        pointsPlayed: 0,
        tournament: "Wimbledon",
      }),
    ]);
    expect(picked).toEqual({
      id: "22222222-2222-4222-8222-222222222222",
      kind: "live",
    });
  });

  it("falls back to the latest completed major then a configured UUID", () => {
    const completed = pickHomeReplayCandidate([
      match({
        id: "44444444-4444-4444-8444-444444444444",
        status: "COMPLETED",
        pointsPlayed: 20,
        tournament: "US Open",
        endedAt: "2026-08-20T12:00:00Z",
      }),
      match({
        id: "55555555-5555-4555-8555-555555555555",
        status: "COMPLETED",
        pointsPlayed: 20,
        tournament: "US Open",
        endedAt: "2026-08-24T12:00:00Z",
      }),
    ]);
    expect(completed?.id).toBe("55555555-5555-4555-8555-555555555555");
    expect(completed?.kind).toBe("replay");
    expect(pickHomeReplayCandidate([], fallback)).toEqual({ id: fallback, kind: "replay" });
    expect(pickHomeReplayCandidate([], "not-a-uuid")).toBeNull();
  });

  it("places lower-tier and junior matches after tour matches", () => {
    const candidates = rankHomeReplayCandidates([
      match({
        id: "11111111-1111-4111-8111-111111111111",
        status: "IN_PROGRESS",
        pointsPlayed: 20,
        tournament: "W15 Bielsko Biala",
      }),
      match({
        id: "22222222-2222-4222-8222-222222222222",
        status: "IN_PROGRESS",
        pointsPlayed: 15,
        tournament: "Pardubicka juniorka",
      }),
      match({
        id: "33333333-3333-4333-8333-333333333333",
        status: "IN_PROGRESS",
        pointsPlayed: 10,
        circuitRank: 1,
        tournament: "Winston-Salem Open",
      }),
    ]);
    expect(candidates).toEqual([
      { id: "33333333-3333-4333-8333-333333333333", kind: "live" },
      { id: "11111111-1111-4111-8111-111111111111", kind: "live" },
      { id: "22222222-2222-4222-8222-222222222222", kind: "live" },
    ]);
  });
});
