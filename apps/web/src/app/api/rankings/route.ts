import { NextResponse } from "next/server";
import {
  fetchUpstreamPlayers,
  fetchUpstreamRankings,
  TennisDataUpstreamError,
  type UpstreamGender,
} from "@/lib/tennis-data-upstream";
import { toPlayersBoard } from "@/lib/rankings-mapper";

/**
 * BFF rankings board — ATP/WTA singles from tennis-data-service.
 * Query: tour=atp|wta (default atp)
 */
export async function GET(request: Request) {
  const tourParam = new URL(request.url).searchParams.get("tour") ?? "atp";
  const tour = tourParam === "wta" ? "wta" : "atp";
  const gender: UpstreamGender = tour === "wta" ? "FEMALE" : "MALE";
  try {
    const [rankings, players] = await Promise.all([
      fetchUpstreamRankings({ gender }),
      fetchUpstreamPlayers({ gender }),
    ]);
    return NextResponse.json(toPlayersBoard(rankings, players, tour), {
      headers: { "Cache-Control": "no-store" },
    });
  } catch (err) {
    const status = err instanceof TennisDataUpstreamError ? err.status ?? 502 : 502;
    return NextResponse.json({ error: "tennis-data-service unavailable" }, { status });
  }
}
