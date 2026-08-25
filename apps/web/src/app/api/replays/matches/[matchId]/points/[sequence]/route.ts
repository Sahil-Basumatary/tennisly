import { NextResponse } from "next/server";
import { isReplayMatchUuid } from "@/lib/replay-index";
import { proxyReplayService } from "@/lib/replay-upstream";

type RouteContext = { params: Promise<{ matchId: string; sequence: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { matchId, sequence } = await context.params;
  if (!isReplayMatchUuid(matchId)) {
    return NextResponse.json({ error: "matchId must be a UUID" }, { status: 400 });
  }
  const sequenceNumber = Number.parseInt(sequence, 10);
  if (!Number.isSafeInteger(sequenceNumber) || sequenceNumber < 1) {
    return NextResponse.json({ error: "sequence must be a positive integer" }, { status: 400 });
  }
  return proxyReplayService(`/api/replays/matches/${matchId}/points/${sequenceNumber}`);
}
