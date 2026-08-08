import { NextResponse } from "next/server";
import { fetchUpstreamPlayerAnalytics } from "@/lib/analytics-upstream";
import { analyticsErrorResponse, noStoreJson } from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ playerId: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { playerId } = await context.params;
  if (!playerId?.trim()) {
    return NextResponse.json({ error: "playerId required" }, { status: 400 });
  }
  const { searchParams } = new URL(request.url);
  try {
    const data = await fetchUpstreamPlayerAnalytics(playerId, {
      from: searchParams.get("from") ?? undefined,
      to: searchParams.get("to") ?? undefined,
      surface: searchParams.get("surface") ?? undefined,
      page: searchParams.get("page") ? Number(searchParams.get("page")) : undefined,
      size: searchParams.get("size") ? Number(searchParams.get("size")) : undefined,
    });
    return noStoreJson(data);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
