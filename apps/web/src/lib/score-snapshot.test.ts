import { describe, expect, it } from "vitest";
import { scoreFromSnapshot, type TapeScore } from "@/lib/score-snapshot";

const fallback: TapeScore = {
  homeSets: [6],
  awaySets: [4],
  homeGames: 3,
  awayGames: 2,
  homePoints: "30",
  awayPoints: "15",
  server: "HOME",
};

describe("scoreFromSnapshot", () => {
  it("parses games, points, and serverId from the live ledger shape", () => {
    const parsed = scoreFromSnapshot(
      {
        games: [
          [6, 4],
          [2, 1],
        ],
        points: ["40", "AD"],
        serverId: "away-id",
      },
      fallback,
      "home-id",
      "away-id",
    );
    expect(parsed.homeSets).toEqual([6]);
    expect(parsed.awaySets).toEqual([4]);
    expect(parsed.homeGames).toBe(2);
    expect(parsed.awayGames).toBe(1);
    expect(parsed.homePoints).toBe("40");
    expect(parsed.awayPoints).toBe("AD");
    expect(parsed.server).toBe("AWAY");
  });

  it("keeps the last trusted board when the snapshot is junk", () => {
    expect(scoreFromSnapshot(undefined, fallback)).toEqual(fallback);
    expect(scoreFromSnapshot({ games: "nope" }, fallback)).toEqual(fallback);
    expect(scoreFromSnapshot(null, fallback)).toEqual(fallback);
  });
});
