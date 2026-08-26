import { jsonPublic, TICKER_CACHE_CONTROL, TICKER_MAX_BYTES } from "@/lib/public-http-cache";
import { tickerUpdatedAt, toScoresFeed } from "@/lib/match-mapper";
import { fetchTickerOriginMatches } from "@/lib/ordered-matches";
import { MatchUpstreamError } from "@/lib/match-upstream";
import { NextResponse } from "next/server";

export async function GET(request: Request) {
  try {
    const matches = await fetchTickerOriginMatches();
    const feed = toScoresFeed(matches, tickerUpdatedAt(matches) || undefined);
    return jsonPublic(request, feed, TICKER_CACHE_CONTROL, { maxBytes: TICKER_MAX_BYTES });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json(
      { error: "match-service unavailable" },
      { status, headers: { "Cache-Control": "private, no-store" } },
    );
  }
}
