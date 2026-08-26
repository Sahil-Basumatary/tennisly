import { describe, expect, it, vi } from "vitest";
import { GET } from "./route";
import { LIVE_CACHE_CONTROL } from "@/lib/public-http-cache";

vi.mock("@/lib/match-upstream", () => ({
  fetchUpstreamLiveCursor: vi.fn(),
  MatchUpstreamError: class MatchUpstreamError extends Error {},
}));

import { fetchUpstreamLiveCursor } from "@/lib/match-upstream";

const cursor = {
  id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  status: "IN_PROGRESS" as const,
  liveSequence: 11,
  pointsPlayed: 8,
};

describe("GET /api/matches/[id]/cursor", () => {
  it("returns only the sequence cursor and is anonymously cacheable", async () => {
    vi.mocked(fetchUpstreamLiveCursor).mockResolvedValue(cursor);
    const response = await GET(
      new Request("http://localhost/api/matches/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee/cursor"),
      { params: Promise.resolve({ id: cursor.id }) },
    );
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(cursor);
    expect(response.headers.get("Cache-Control")).toBe(LIVE_CACHE_CONTROL);
    expect(response.headers.get("ETag")).toBe(`"cursor-${cursor.id}-11"`);
    expect(response.headers.get("Vary")).toBe("Accept-Encoding");
  });
});
