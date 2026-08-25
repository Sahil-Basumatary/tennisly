import { NextResponse } from "next/server";
import { isReplayMatchUuid } from "@/lib/replay-index";
import { proxyReplayService } from "@/lib/replay-upstream";

type RouteContext = { params: Promise<{ matchId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { matchId } = await context.params;
  if (!isReplayMatchUuid(matchId)) {
    return NextResponse.json({ error: "matchId must be a UUID" }, { status: 400 });
  }
  return proxyReplayService(`/api/replays/matches/${matchId}`);
}
