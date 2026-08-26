import { NextResponse } from "next/server";
import { fetchUpstreamMatch, MatchUpstreamError } from "@/lib/match-upstream";

type RouteContext = { params: Promise<{ id: string }> };

/**
 * BFF match detail — accepts UUID or broadcast externalId.
 */
export async function GET(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!id?.trim()) {
    return NextResponse.json({ error: "id required" }, { status: 400 });
  }
  try {
    const match = await fetchUpstreamMatch(id);
    if (!match) {
      return NextResponse.json({ error: "match not found" }, { status: 404 });
    }
    return NextResponse.json(match, {
      headers: { "Cache-Control": "private, no-store, no-cache, must-revalidate" },
    });
  } catch (err) {
    const status = err instanceof MatchUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json({ error: "match-service unavailable" }, { status });
  }
}
