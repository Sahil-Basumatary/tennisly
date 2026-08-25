import { compareMatchesByPriority } from "@/lib/match-order";
import { fetchUpstreamMatches } from "@/lib/match-upstream";
import type { UpstreamMatchStatus } from "@/types/match-catalogue";

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
      fetchUpstreamMatches({ status, page: 0, size: 100 }),
    ),
  );
  return [...new Map(boards.flat().map((match) => [match.id, match])).values()]
    .sort(compareMatchesByPriority)
    .slice(0, pageSize);
}
