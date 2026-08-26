import {
  COMPLETED_LIVE_CACHE_CONTROL,
  LIVE_CACHE_CONTROL,
  LIVE_MAX_BYTES,
  PRIVATE_NO_STORE,
  jsonPublic,
  sequenceEtag,
} from "@/lib/public-http-cache";
import { isTerminalMatchStatus } from "@/lib/live-score-document";
import { fetchUpstreamLiveScore, MatchUpstreamError } from "@/lib/match-upstream";
import { NextResponse } from "next/server";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!id?.trim()) {
    return NextResponse.json({ error: "id required" }, { status: 400 });
  }
  try {
    const live = await fetchUpstreamLiveScore(id);
    if (!live) {
      return NextResponse.json(
        { error: "match not found" },
        { status: 404, headers: { "Cache-Control": PRIVATE_NO_STORE } },
      );
    }
    const cacheControl = isTerminalMatchStatus(live.status)
      ? COMPLETED_LIVE_CACHE_CONTROL
      : LIVE_CACHE_CONTROL;
    return jsonPublic(request, live, cacheControl, {
      etag: sequenceEtag(`live-${live.id}`, live.liveSequence),
      maxBytes: LIVE_MAX_BYTES,
    });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json(
      { error: "match-service unavailable" },
      { status, headers: { "Cache-Control": PRIVATE_NO_STORE } },
    );
  }
}
