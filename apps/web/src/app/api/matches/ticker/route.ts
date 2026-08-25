import { NextResponse } from "next/server";
import { MatchUpstreamError } from "@/lib/match-upstream";
import { toScoresFeed } from "@/lib/match-mapper";
import { fetchOrderedMatches } from "@/lib/ordered-matches";
import { withScoresFeedHeadshots } from "@/lib/player-photos";

export async function GET() {
  try {
    const live = await fetchOrderedMatches("live", 12);
    const feed =
      live.length > 0
        ? await withScoresFeedHeadshots(toScoresFeed(live))
        : await withScoresFeedHeadshots(toScoresFeed(await fetchOrderedMatches(undefined, 12)));
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
