import { afterEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";
import { TICKER_CACHE_CONTROL } from "@/lib/public-http-cache";
import { resetSingleFlight } from "@/lib/single-flight";

vi.mock("@/lib/ordered-matches", () => ({
  fetchTickerOriginMatches: vi.fn(),
}));

import { fetchTickerOriginMatches } from "@/lib/ordered-matches";

const tickerMatches = [
  {
    id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
    surface: "HARD" as const,
    status: "IN_PROGRESS" as const,
    bestOfSets: 3,
    updatedAt: "2026-08-26T12:00:00Z",
    currentScore: { points: ["15", "0"] },
    metadata: { tournamentName: "US Open", tournamentShortName: "USO" },
    players: [
      { id: "h", playerId: "hp", displayName: "Home Player", side: "HOME" as const },
      { id: "a", playerId: "ap", displayName: "Away Player", side: "AWAY" as const },
    ],
    pointsPlayed: 4,
    liveSequence: 6,
  },
];

afterEach(() => {
  resetSingleFlight();
  vi.clearAllMocks();
});

describe("GET /api/matches/ticker", () => {
  it("is anonymously cacheable and answers 304 on the same etag", async () => {
    vi.mocked(fetchTickerOriginMatches).mockResolvedValue(tickerMatches);
    const first = await GET(new Request("http://localhost/api/matches/ticker"));
    expect(first.status).toBe(200);
    expect(first.headers.get("Cache-Control")).toBe(TICKER_CACHE_CONTROL);
    expect(first.headers.get("Vary")).toBe("Accept-Encoding");
    const etag = first.headers.get("ETag");
    expect(etag).toMatch(/^"[0-9a-f]{16}"$/);
    const second = await GET(
      new Request("http://localhost/api/matches/ticker", {
        headers: { "If-None-Match": etag ?? "" },
      }),
    );
    expect(second.status).toBe(304);
    expect(second.headers.get("ETag")).toBe(etag);
  });
});
