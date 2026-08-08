import { NextResponse } from "next/server";
import { fetchUpstreamMatchCsv } from "@/lib/analytics-upstream";
import { analyticsErrorResponse } from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ matchId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { matchId } = await context.params;
  if (!matchId?.trim()) {
    return NextResponse.json({ error: "matchId required" }, { status: 400 });
  }
  try {
    const csv = await fetchUpstreamMatchCsv(matchId);
    return new NextResponse(csv, {
      headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": `attachment; filename="match-${matchId}-analytics.csv"`,
        "Cache-Control": "no-store",
      },
    });
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
