import {
  COMPLETED_LIVE_CACHE_CONTROL,
  LIVE_CACHE_CONTROL,
  LIVE_MAX_BYTES,
  PRIVATE_NO_STORE,
  jsonPublic,
  sequenceEtag,
} from "@/lib/public-http-cache";
import { isTerminalMatchStatus } from "@/lib/live-score-document";
import { fetchUpstreamLiveCursor, MatchUpstreamError } from "@/lib/match-upstream";
import { NextResponse } from "next/server";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!id?.trim()) {
    return NextResponse.json({ error: "id required" }, { status: 400 });
  }
  try {
    const cursor = await fetchUpstreamLiveCursor(id);
    if (!cursor) {
      return NextResponse.json(
        { error: "match not found" },
        { status: 404, headers: { "Cache-Control": PRIVATE_NO_STORE } },
      );
    }
    const cacheControl = isTerminalMatchStatus(cursor.status)
      ? COMPLETED_LIVE_CACHE_CONTROL
      : LIVE_CACHE_CONTROL;
    return jsonPublic(request, cursor, cacheControl, {
      etag: sequenceEtag(`cursor-${cursor.id}`, cursor.liveSequence),
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
