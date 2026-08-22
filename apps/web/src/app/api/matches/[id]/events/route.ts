import { NextResponse } from "next/server";
import { fetchUpstreamMatchEvents, MatchUpstreamError } from "@/lib/match-upstream";
import { isReplayMatchUuid } from "@/lib/replay-index";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!isReplayMatchUuid(id)) {
    return NextResponse.json({ error: "match id must be a UUID" }, { status: 400 });
  }
  const url = new URL(request.url);
  const afterSequence = Number(url.searchParams.get("afterSequence") ?? "0");
  const limit = Number(url.searchParams.get("limit") ?? "100");
  if (!Number.isFinite(afterSequence) || afterSequence < 0 || !Number.isFinite(limit)) {
    return NextResponse.json({ error: "afterSequence and limit must be numbers" }, { status: 400 });
  }
  try {
    const events = await fetchUpstreamMatchEvents(id, afterSequence, limit);
    return NextResponse.json(events, { headers: { "Cache-Control": "no-store" } });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json({ error: "match-service unavailable" }, { status });
  }
}
