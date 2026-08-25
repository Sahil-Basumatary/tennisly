import { describe, expect, it } from "vitest";
import { advanceCursorFromEvents, unseenPointSequences } from "@/lib/live-replay-sync";
import { normalizePointReplay } from "@/services/replay";

describe("live replay sync", () => {
  it("diffs unseen sequences from the point ledger", () => {
    expect(
      unseenPointSequences([1, 2], [
        { sequenceNumber: 2 },
        { sequenceNumber: 4 },
        { sequence: 3 },
        { sequenceNumber: "nope" },
      ]),
    ).toEqual([3, 4]);
  });

  it("advances the reconnect cursor to the highest proven event sequence", () => {
    expect(advanceCursorFromEvents(4, [{ sequence: 2 }, { sequence: 9 }, { sequence: "x" }])).toBe(9);
    expect(advanceCursorFromEvents(4, [])).toBe(4);
  });
});

describe("normalizePointReplay", () => {
  const goodFrame = {
    timeSeconds: 0,
    ball: { x: 0, y: 0, z: 1 },
    home: { x: 0, y: -11, z: 0 },
    away: { x: 0, y: 11, z: 0 },
    pointSequence: 1,
    shotIndex: 0,
    shotType: "FIRST_SERVE",
  };

  it("rejects malformed payloads instead of inventing a rally", () => {
    expect(normalizePointReplay(null)).toBeNull();
    expect(normalizePointReplay({})).toBeNull();
    expect(normalizePointReplay({ point: { sequence: 0 }, frames: [goodFrame] })).toBeNull();
    expect(
      normalizePointReplay({
        point: { sequence: 1, serverId: "a", winnerId: "b" },
        frames: [{ timeSeconds: 0, ball: { x: "nope" } }],
      }),
    ).toBeNull();
  });

  it("accepts a well-formed point replay", () => {
    const parsed = normalizePointReplay({
      matchId: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
      surface: "GRASS",
      point: { sequence: 1, serverId: "a", winnerId: "b", outcome: "ACE" },
      shots: [],
      frames: [goodFrame],
    });
    expect(parsed?.point.sequence).toBe(1);
    expect(parsed?.frames).toHaveLength(1);
  });
});
