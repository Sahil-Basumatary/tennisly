import { NextResponse } from "next/server";
import { fetchUpstreamPlayerCsv } from "@/lib/analytics-upstream";
import { analyticsErrorResponse } from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ playerId: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { playerId } = await context.params;
  if (!playerId?.trim()) {
    return NextResponse.json({ error: "playerId required" }, { status: 400 });
  }
  const { searchParams } = new URL(request.url);
  try {
    const csv = await fetchUpstreamPlayerCsv(playerId, {
      from: searchParams.get("from") ?? undefined,
      to: searchParams.get("to") ?? undefined,
      surface: searchParams.get("surface") ?? undefined,
    });
    return new NextResponse(csv, {
      headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": `attachment; filename="player-${playerId}-analytics.csv"`,
        "Cache-Control": "no-store",
      },
    });
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
