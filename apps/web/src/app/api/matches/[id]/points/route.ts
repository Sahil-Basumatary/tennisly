import { NextResponse } from "next/server";
import { fetchUpstreamMatchPoints, MatchUpstreamError } from "@/lib/match-upstream";
import { isReplayMatchUuid } from "@/lib/replay-index";

type RouteContext = { params: Promise<{ id: string }> };

/** BFF point ledger for match-centre box scores. */
export async function GET(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!isReplayMatchUuid(id)) {
    return NextResponse.json({ error: "match id must be a UUID" }, { status: 400 });
  }
  try {
    const points = await fetchUpstreamMatchPoints(id);
    return NextResponse.json(points, {
      headers: { "Cache-Control": "private, no-store, no-cache, must-revalidate" },
    });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json({ error: "match-service unavailable" }, { status });
  }
}
