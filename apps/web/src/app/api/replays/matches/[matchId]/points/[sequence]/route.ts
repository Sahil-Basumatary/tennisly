import { NextResponse } from "next/server";
import { isReplayMatchUuid } from "@/lib/replay-index";
import { isSealedPoint, REPLAY_ENGINE_VERSION } from "@/lib/replay-cache-policy";
import { fetchUpstreamLiveCursor } from "@/lib/match-upstream";
import { proxyReplayPoint } from "@/lib/replay-upstream";

type RouteContext = { params: Promise<{ matchId: string; sequence: string }> };

export async function GET(request: Request, context: RouteContext) {
  const { matchId, sequence } = await context.params;
  if (!isReplayMatchUuid(matchId)) {
    return NextResponse.json({ error: "matchId must be a UUID" }, { status: 400 });
  }
  const sequenceNumber = Number.parseInt(sequence, 10);
  if (!Number.isSafeInteger(sequenceNumber) || sequenceNumber < 1) {
    return NextResponse.json({ error: "sequence must be a positive integer" }, { status: 400 });
  }
  const engine = new URL(request.url).searchParams.get("engine");
  let pointsPlayed = 0;
  try {
    const cursor = await fetchUpstreamLiveCursor(matchId);
    pointsPlayed = cursor?.pointsPlayed ?? 0;
  } catch {
    pointsPlayed = 0;
  }
  return proxyReplayPoint({
    request,
    matchId,
    sequence: sequenceNumber,
    engine: engine && engine.length > 0 ? engine : REPLAY_ENGINE_VERSION,
    sealed: isSealedPoint(sequenceNumber, pointsPlayed),
  });
}
