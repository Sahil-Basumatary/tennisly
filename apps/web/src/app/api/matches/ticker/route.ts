import { NextResponse } from "next/server";
import { fetchUpstreamMatches, MatchUpstreamError } from "@/lib/match-upstream";
import { toScoresFeed } from "@/lib/match-mapper";

export async function GET() {
  try {
    const live = await fetchUpstreamMatches({ status: "IN_PROGRESS", page: 0, size: 12 });
    const feed =
      live.length > 0
        ? toScoresFeed(live)
        : toScoresFeed(await fetchUpstreamMatches({ page: 0, size: 12 }));
    return NextResponse.json(feed, {
      headers: {
        "Cache-Control": "private, max-age=5, stale-while-revalidate=15",
      },
    });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json(
      { updatedAt: new Date().toISOString(), items: [] },
      { status: status === 502 ? 200 : status },
    );
  }
}
