import { NextResponse } from "next/server";
import { unstable_cache } from "next/cache";

export function replayServiceBase(): string {
  return (process.env.REPLAY_SERVICE_URL ?? "http://localhost:8085").replace(/\/$/, "");
}

const probeReplayPoint = unstable_cache(
  async (matchId: string): Promise<boolean> => {
    try {
      const response = await fetch(
        `${replayServiceBase()}/api/replays/matches/${matchId}/points/1`,
        {
          headers: { Accept: "application/json" },
          signal: AbortSignal.timeout(2_500),
        },
      );
      await response.body?.cancel();
      return response.ok;
    } catch {
      return false;
    }
  },
  ["home-replay-point-ready"],
  { revalidate: 60 },
);

export async function isReplayPointReady(matchId: string): Promise<boolean> {
  return probeReplayPoint(matchId);
}

/** Browser never talks to replay-service; the BFF copies status and JSON through. */
export async function proxyReplayService(path: string): Promise<NextResponse> {
  let upstream: Response;
  try {
    upstream = await fetch(`${replayServiceBase()}${path}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
  } catch {
    return NextResponse.json({ error: "replay-service unreachable" }, { status: 502 });
  }
  const body = await upstream.text();
  return new NextResponse(body, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("Content-Type") ?? "application/json",
    },
  });
}
