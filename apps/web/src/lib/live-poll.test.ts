import { describe, expect, it } from "vitest";
import {
  LIVE_HIDDEN_MS,
  LIVE_VISIBLE_MS,
  clampJitter,
  livePollIntervalMs,
  liveTransport,
  reconnectDelayMs,
} from "@/lib/live-poll";

describe("live poll", () => {
  it("defaults public transport to HTTP and keeps WebSocket opt-in", () => {
    expect(liveTransport(undefined)).toBe("http");
    expect(liveTransport("http")).toBe("http");
    expect(liveTransport("hybrid")).toBe("hybrid");
  });

  it("uses a 3s visible cadence and slows hidden tabs", () => {
    expect(livePollIntervalMs(false, 0)).toBe(LIVE_VISIBLE_MS);
    expect(livePollIntervalMs(true, 50)).toBe(LIVE_HIDDEN_MS + 50);
  });

  it("backs off reconnects without exceeding 5s", () => {
    expect(reconnectDelayMs(0)).toBe(400);
    expect(reconnectDelayMs(1)).toBe(800);
    expect(reconnectDelayMs(2)).toBe(1600);
    expect(reconnectDelayMs(8)).toBe(5_000);
  });

  it("keeps jitter inside the requested bound", () => {
    expect(clampJitter(0, 400)).toBe(0);
    expect(clampJitter(1, 400)).toBe(400);
  });
});
