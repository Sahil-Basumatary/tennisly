import { compareMatchesByPriority } from "@/lib/match-order";
import { fetchUpstreamMatches, fetchUpstreamTicker } from "@/lib/match-upstream";
import { singleFlight } from "@/lib/single-flight";
import type { UpstreamMatch, UpstreamMatchStatus } from "@/types/match-catalogue";

function statusesFor(uiStatus?: string): UpstreamMatchStatus[] {
  if (uiStatus === "live") return ["IN_PROGRESS", "SUSPENDED"];
  if (uiStatus === "upcoming") return ["SCHEDULED"];
  if (uiStatus === "final") return ["COMPLETED"];
  return ["IN_PROGRESS", "SUSPENDED", "SCHEDULED", "COMPLETED"];
}

export async function fetchOrderedMatches(uiStatus?: string, limit = 50) {
  const pageSize = Math.max(1, Math.min(100, limit));
  const boards = await Promise.all(
    statusesFor(uiStatus).map((status) =>
      fetchUpstreamMatches({ status, page: 0, size: pageSize }),
    ),
  );
  return [...new Map(boards.flat().map((match) => [match.id, match])).values()]
    .sort(compareMatchesByPriority)
    .slice(0, pageSize);
}

export async function fetchTickerOriginMatches(): Promise<UpstreamMatch[]> {
  return singleFlight("match-ticker", async () => {
    try {
      const cached = await fetchUpstreamTicker();
      if (cached && cached.length > 0) return cached;
    } catch {
      // Redis ticker is an optimisation; the strip still has a bounded catalogue path.
    }
    const live = await fetchOrderedMatches("live", 12);
    if (live.length > 0) return live;
    return fetchOrderedMatches("final", 12);
  });
}
