import { describe, expect, it, vi } from "vitest";
import { GET } from "./route";
import { COMPLETED_LIVE_CACHE_CONTROL, LIVE_CACHE_CONTROL } from "@/lib/public-http-cache";

vi.mock("@/lib/match-upstream", () => ({
  fetchUpstreamLiveScore: vi.fn(),
  MatchUpstreamError: class MatchUpstreamError extends Error {},
}));

import { fetchUpstreamLiveScore } from "@/lib/match-upstream";

const liveMatch = {
  id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  status: "IN_PROGRESS" as const,
  liveSequence: 9,
  pointsPlayed: 7,
  updatedAt: "2026-08-26T12:00:00Z",
  currentScore: { points: ["40", "30"] },
};

describe("GET /api/matches/[id]/live", () => {
  it("caches live matches briefly and keys the etag on liveSequence", async () => {
    vi.mocked(fetchUpstreamLiveScore).mockResolvedValue(liveMatch);
    const request = new Request("http://localhost/api/matches/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee/live");
    const response = await GET(request, {
      params: Promise.resolve({ id: liveMatch.id }),
    });
    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe(LIVE_CACHE_CONTROL);
    expect(response.headers.get("ETag")).toBe(`"live-${liveMatch.id}-9"`);
    const again = await GET(
      new Request(request.url, { headers: { "If-None-Match": `"live-${liveMatch.id}-9"` } }),
      { params: Promise.resolve({ id: liveMatch.id }) },
    );
    expect(again.status).toBe(304);
  });

  it("uses a long immutable policy for completed matches", async () => {
    vi.mocked(fetchUpstreamLiveScore).mockResolvedValue({
      ...liveMatch,
      status: "COMPLETED",
      liveSequence: 40,
    });
    const response = await GET(
      new Request("http://localhost/api/matches/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee/live"),
      { params: Promise.resolve({ id: liveMatch.id }) },
    );
    expect(response.headers.get("Cache-Control")).toBe(COMPLETED_LIVE_CACHE_CONTROL);
    expect(response.headers.get("ETag")).toBe(`"live-${liveMatch.id}-40"`);
  });
});
