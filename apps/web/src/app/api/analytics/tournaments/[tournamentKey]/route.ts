import { NextResponse } from "next/server";
import { fetchUpstreamTournamentAnalytics } from "@/lib/analytics-upstream";
import { analyticsErrorResponse, noStoreJson } from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ tournamentKey: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { tournamentKey } = await context.params;
  const decoded = decodeURIComponent(tournamentKey ?? "");
  if (!decoded.trim()) {
    return NextResponse.json({ error: "tournamentKey required" }, { status: 400 });
  }
  try {
    const data = await fetchUpstreamTournamentAnalytics(decoded);
    return noStoreJson(data);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
