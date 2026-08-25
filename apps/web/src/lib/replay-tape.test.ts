import { describe, expect, it } from "vitest";
import {
  appendPointToTape,
  densifyShotIndexes,
  prepareHydratedTape,
  tapeDurationSeconds,
  trimTapeToLastPoints,
  type ReplayTape,
} from "@/lib/replay-tape";
import type { PointReplay, PointSummary, ReplayFrame, ShotSummary } from "@/types/replay";

function vec(x = 0, y = 0, z = 0) {
  return { x, y, z };
}

function frame(over: Partial<ReplayFrame> = {}): ReplayFrame {
  return {
    timeSeconds: 0,
    ball: vec(0, 0, 1),
    home: vec(0, -11, 0),
    away: vec(0, 11, 0),
    pointSequence: 1,
    shotIndex: 0,
    shotType: "FIRST_SERVE",
    ...over,
  };
}

function shot(over: Partial<ShotSummary> = {}): ShotSummary {
  return {
    pointSequence: 1,
    shotIndex: 0,
    shotType: "FIRST_SERVE",
    hitter: "HOME",
    spin: "FLAT",
    contact: vec(),
    landing: vec(1, 4, 0),
    launchSpeedKmh: 180,
    apexHeightMetres: 2,
    flightSeconds: 0.8,
    ...over,
  };
}

function point(sequence: number): PointSummary {
  return {
    sequence,
    serverId: "home",
    winnerId: "home",
    outcome: "WINNER",
    rallyLength: 1,
    shotCount: 1,
    durationSeconds: 1,
  };
}

function pointReplay(sequence: number, localShot = 0): PointReplay {
  return {
    matchId: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
    surface: "GRASS",
    frameRate: 60,
    point: point(sequence),
    shots: [shot({ pointSequence: sequence, shotIndex: localShot })],
    frames: [
      frame({ pointSequence: sequence, shotIndex: localShot, timeSeconds: 0 }),
      frame({ pointSequence: sequence, shotIndex: localShot, timeSeconds: 1 }),
    ],
  };
}

function tape(points: number[]): ReplayTape {
  return {
    points: points.map(point),
    shots: points.map((sequence) => shot({ pointSequence: sequence, shotIndex: 0 })),
    frames: points.flatMap((sequence, i) => [
      frame({ pointSequence: sequence, shotIndex: 0, timeSeconds: i }),
      frame({ pointSequence: sequence, shotIndex: 0, timeSeconds: i + 0.9 }),
    ]),
  };
}

describe("replay tape", () => {
  it("densifies per-point shot indexes onto a match clock", () => {
    const dense = densifyShotIndexes({
      points: [point(1), point(2)],
      shots: [
        shot({ pointSequence: 1, shotIndex: 0 }),
        shot({ pointSequence: 2, shotIndex: 0 }),
      ],
      frames: [
        frame({ pointSequence: 1, shotIndex: 0, timeSeconds: 0 }),
        frame({ pointSequence: 2, shotIndex: 0, timeSeconds: 1 }),
      ],
    });
    expect(dense.frames.map((f) => f.shotIndex)).toEqual([0, 1]);
    expect(dense.shots.map((s) => s.shotIndex)).toEqual([0, 1]);
  });

  it("rebases incoming point time and rejects duplicates", () => {
    const first = appendPointToTape(
      { frames: [], shots: [], points: [] },
      pointReplay(1),
    );
    expect(first).not.toBeNull();
    expect(tapeDurationSeconds(first!)).toBe(1);
    const second = appendPointToTape(first!, pointReplay(2));
    expect(second).not.toBeNull();
    expect(second!.points.map((p) => p.sequence)).toEqual([1, 2]);
    expect(second!.frames[0]?.timeSeconds).toBe(0);
    expect(second!.frames.at(-1)?.timeSeconds).toBe(2);
    expect(second!.shots.map((s) => s.shotIndex)).toEqual([0, 1]);
    expect(appendPointToTape(second!, pointReplay(2))).toBeNull();
  });

  it("drops the oldest points when the rolling buffer fills", () => {
    const long = prepareHydratedTape(tape([1, 2, 3]), 2);
    expect(long.points.map((p) => p.sequence)).toEqual([2, 3]);
    expect(long.frames[0]?.timeSeconds).toBe(0);
  });

  it("rejects a point with no frames", () => {
    const empty: PointReplay = { ...pointReplay(1), frames: [] };
    expect(appendPointToTape({ frames: [], shots: [], points: [] }, empty)).toBeNull();
  });

  it("trim keeps duration starting at zero", () => {
    const trimmed = trimTapeToLastPoints(prepareHydratedTape(tape([1, 2, 3])), 1);
    expect(trimmed.points).toHaveLength(1);
    expect(trimmed.frames[0]?.timeSeconds).toBe(0);
  });
});
