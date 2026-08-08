import { NextResponse } from "next/server";
import { fetchUpstreamCompare } from "@/lib/analytics-upstream";
import { analyticsErrorResponse, noStoreJson } from "@/lib/analytics-bff";

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const playerA = searchParams.get("playerA");
  const playerB = searchParams.get("playerB");
  if (!playerA?.trim() || !playerB?.trim()) {
    return NextResponse.json({ error: "playerA and playerB required" }, { status: 400 });
  }
  try {
    const data = await fetchUpstreamCompare(playerA, playerB, {
      from: searchParams.get("from") ?? undefined,
      to: searchParams.get("to") ?? undefined,
    });
    return noStoreJson(data);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
