import { describe, expect, it } from "vitest";
import {
  REPLAY_ENGINE_VERSION,
  isSealedPoint,
  replayEngineMatches,
} from "@/lib/replay-cache-policy";
import {
  SEALED_REPLAY_CACHE_CONTROL,
  TRAILING_REPLAY_CACHE_CONTROL,
} from "@/lib/public-http-cache";

describe("replay cache policy", () => {
  it("seals every point except the newest committed sequence", () => {
    expect(isSealedPoint(1, 4)).toBe(true);
    expect(isSealedPoint(3, 4)).toBe(true);
    expect(isSealedPoint(4, 4)).toBe(false);
    expect(isSealedPoint(5, 4)).toBe(false);
  });

  it("rejects cross-version cache collisions", () => {
    expect(replayEngineMatches(null, REPLAY_ENGINE_VERSION)).toBe(true);
    expect(replayEngineMatches("2.0.0", "2.0.0")).toBe(true);
    expect(replayEngineMatches("1.0.0", "2.0.0")).toBe(false);
    expect(SEALED_REPLAY_CACHE_CONTROL).toContain("immutable");
    expect(TRAILING_REPLAY_CACHE_CONTROL).toContain("s-maxage=2");
  });
});
