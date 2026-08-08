import { NextResponse } from "next/server";
import { fetchUpstreamMatchReport } from "@/lib/analytics-upstream";
import { analyticsErrorResponse, noStoreJson } from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ matchId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { matchId } = await context.params;
  if (!matchId?.trim()) {
    return NextResponse.json({ error: "matchId required" }, { status: 400 });
  }
  try {
    const data = await fetchUpstreamMatchReport(matchId);
    return noStoreJson(data);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
