import { describe, expect, it } from "vitest";
import {
  advanceCursorFromEvents,
  missingPointRange,
  needsEventRecovery,
  unseenPointSequences,
} from "@/lib/live-replay-sync";
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

  it("fills cursor gaps from pointsPlayed without a full ledger", () => {
    expect(missingPointRange([1, 2], 5)).toEqual([3, 4, 5]);
    expect(missingPointRange([1, 3], 4)).toEqual([2, 4]);
    expect(missingPointRange([1, 2, 3], 3)).toEqual([]);
    expect(missingPointRange([], 20, 4)).toEqual([1, 2, 3, 4]);
  });

  it("suppresses duplicate sequences already on the tape", () => {
    expect(unseenPointSequences([1, 2, 3], [{ sequenceNumber: 2 }, { sequenceNumber: 3 }])).toEqual(
      [],
    );
  });

  it("skips event recovery on join and on a single-sequence advance", () => {
    expect(needsEventRecovery(0, 12)).toBe(false);
    expect(needsEventRecovery(4, 5)).toBe(false);
    expect(needsEventRecovery(4, 4)).toBe(false);
    expect(needsEventRecovery(4, 6)).toBe(true);
    expect(needsEventRecovery(4, 20)).toBe(true);
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
