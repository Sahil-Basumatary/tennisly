import { NextResponse } from "next/server";
import { fetchUpstreamMatches, MatchUpstreamError } from "@/lib/match-upstream";
import type { UpstreamMatchStatus } from "@/types/match-catalogue";

const STATUSES = new Set<UpstreamMatchStatus>([
  "SCHEDULED",
  "IN_PROGRESS",
  "SUSPENDED",
  "COMPLETED",
  "CANCELLED",
]);

/**
 * BFF catalogue list — browser never talks to match-service :8084 directly.
 */
export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const statusParam = searchParams.get("status") ?? undefined;
  const tournamentId = searchParams.get("tournamentId") ?? undefined;
  if (statusParam && !STATUSES.has(statusParam as UpstreamMatchStatus)) {
    return NextResponse.json({ error: "invalid status" }, { status: 400 });
  }
  try {
    const matches = await fetchUpstreamMatches({
      status: statusParam as UpstreamMatchStatus | undefined,
      tournamentId: tournamentId ?? undefined,
    });
    return NextResponse.json(matches, {
      headers: { "Cache-Control": "no-store" },
    });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json({ error: "match-service unavailable" }, { status });
  }
}
