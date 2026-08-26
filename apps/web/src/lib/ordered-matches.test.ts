import { describe, expect, it, vi } from "vitest";
import { fetchOrderedMatches } from "@/lib/ordered-matches";

vi.mock("@/lib/match-upstream", () => ({
  fetchUpstreamMatches: vi.fn(),
  fetchUpstreamTicker: vi.fn(),
}));

import { fetchUpstreamMatches } from "@/lib/match-upstream";

describe("fetchOrderedMatches", () => {
  it("asks match-service for the requested page size instead of 100-row catalogues", async () => {
    vi.mocked(fetchUpstreamMatches).mockResolvedValue([]);
    await fetchOrderedMatches("live", 12);
    expect(fetchUpstreamMatches).toHaveBeenCalledTimes(2);
    expect(fetchUpstreamMatches).toHaveBeenCalledWith({
      status: "IN_PROGRESS",
      page: 0,
      size: 12,
    });
    expect(fetchUpstreamMatches).toHaveBeenCalledWith({
      status: "SUSPENDED",
      page: 0,
      size: 12,
    });
  });
});
