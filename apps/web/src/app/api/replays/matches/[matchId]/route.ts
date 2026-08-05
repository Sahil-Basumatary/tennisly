import { NextResponse } from "next/server";
import { isReplayMatchUuid } from "@/lib/replay-index";

type RouteContext = { params: Promise<{ matchId: string }> };

/**
 * BFF to replay-service so the browser never talks to :8085 directly.
 * Scaffold match ids are rejected here — the client falls back to the mock.
 */
export async function GET(_request: Request, context: RouteContext) {
  const { matchId } = await context.params;
  if (!isReplayMatchUuid(matchId)) {
    return NextResponse.json({ error: "matchId must be a UUID" }, { status: 400 });
  }

  const base = (process.env.REPLAY_SERVICE_URL ?? "http://localhost:8085").replace(/\/$/, "");
  let upstream: Response;
  try {
    upstream = await fetch(`${base}/api/replays/matches/${matchId}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: "replay-service unreachable" },
      { status: 502 },
    );
  }

  const body = await upstream.text();
  return new NextResponse(body, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("Content-Type") ?? "application/json",
    },
  });
}
