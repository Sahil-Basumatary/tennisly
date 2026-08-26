import { describe, expect, it, vi } from "vitest";
import { GET } from "./route";
import { SEALED_REPLAY_CACHE_CONTROL, TRAILING_REPLAY_CACHE_CONTROL } from "@/lib/public-http-cache";
import { REPLAY_ENGINE_VERSION } from "@/lib/replay-cache-policy";

vi.mock("@/lib/match-upstream", () => ({
  fetchUpstreamLiveCursor: vi.fn(),
}));

vi.mock("@/lib/replay-upstream", async () => {
  const actual = await vi.importActual<typeof import("@/lib/replay-upstream")>(
    "@/lib/replay-upstream",
  );
  return {
    ...actual,
    replayServiceBase: () => "http://replay.test",
  };
});

import { fetchUpstreamLiveCursor } from "@/lib/match-upstream";

const MATCH = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

function pointBody(engine = REPLAY_ENGINE_VERSION) {
  return {
    matchId: MATCH,
    surface: "GRASS",
    frameRate: 60,
    engineVersion: engine,
    point: { sequence: 1, serverId: "a", winnerId: "b", outcome: "ACE" },
    shots: [],
    frames: [],
  };
}

describe("GET /api/replays/matches/[matchId]/points/[sequence]", () => {
  it("caches sealed points immutably and rejects a cross-version engine", async () => {
    vi.mocked(fetchUpstreamLiveCursor).mockResolvedValue({
      id: MATCH,
      status: "IN_PROGRESS",
      liveSequence: 4,
      pointsPlayed: 4,
    });
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(pointBody()), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const sealed = await GET(
      new Request(`http://localhost/api/replays/matches/${MATCH}/points/1?engine=${REPLAY_ENGINE_VERSION}`),
      { params: Promise.resolve({ matchId: MATCH, sequence: "1" }) },
    );
    expect(sealed.status).toBe(200);
    expect(sealed.headers.get("Cache-Control")).toBe(SEALED_REPLAY_CACHE_CONTROL);
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(pointBody("1.0.0")), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const mismatch = await GET(
      new Request(`http://localhost/api/replays/matches/${MATCH}/points/1?engine=${REPLAY_ENGINE_VERSION}`),
      { params: Promise.resolve({ matchId: MATCH, sequence: "1" }) },
    );
    expect(mismatch.status).toBe(409);
    expect(mismatch.headers.get("Cache-Control")).toContain("private");
    fetchMock.mockRestore();
  });

  it("keeps the newest point on a short TTL", async () => {
    vi.mocked(fetchUpstreamLiveCursor).mockResolvedValue({
      id: MATCH,
      status: "IN_PROGRESS",
      liveSequence: 4,
      pointsPlayed: 4,
    });
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(pointBody()), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const trailing = await GET(
      new Request(`http://localhost/api/replays/matches/${MATCH}/points/4?engine=${REPLAY_ENGINE_VERSION}`),
      { params: Promise.resolve({ matchId: MATCH, sequence: "4" }) },
    );
    expect(trailing.headers.get("Cache-Control")).toBe(TRAILING_REPLAY_CACHE_CONTROL);
    fetchMock.mockRestore();
  });
});
