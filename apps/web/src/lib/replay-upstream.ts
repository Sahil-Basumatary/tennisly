import { NextResponse } from "next/server";
import { unstable_cache } from "next/cache";
import {
  PRIVATE_NO_STORE,
  SEALED_REPLAY_CACHE_CONTROL,
  TRAILING_REPLAY_CACHE_CONTROL,
  jsonPublic,
} from "@/lib/public-http-cache";
import { REPLAY_ENGINE_VERSION, replayEngineMatches } from "@/lib/replay-cache-policy";

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
      "Cache-Control": PRIVATE_NO_STORE,
    },
  });
}

export async function proxyReplayPoint(options: {
  request: Request;
  matchId: string;
  sequence: number;
  engine: string | null;
  sealed: boolean;
}): Promise<NextResponse> {
  let upstream: Response;
  try {
    upstream = await fetch(
      `${replayServiceBase()}/api/replays/matches/${options.matchId}/points/${options.sequence}`,
      {
        headers: { Accept: "application/json" },
        cache: "no-store",
      },
    );
  } catch {
    return NextResponse.json(
      { error: "replay-service unreachable" },
      { status: 502, headers: { "Cache-Control": PRIVATE_NO_STORE } },
    );
  }
  const body = await upstream.text();
  if (!upstream.ok) {
    return new NextResponse(body, {
      status: upstream.status,
      headers: {
        "Content-Type": upstream.headers.get("Content-Type") ?? "application/json",
        "Cache-Control": PRIVATE_NO_STORE,
      },
    });
  }
  let parsed: { engineVersion?: string };
  try {
    parsed = JSON.parse(body) as { engineVersion?: string };
  } catch {
    return NextResponse.json(
      { error: "invalid replay payload" },
      { status: 502, headers: { "Cache-Control": PRIVATE_NO_STORE } },
    );
  }
  if (!replayEngineMatches(options.engine, parsed.engineVersion)) {
    return NextResponse.json(
      {
        error: "replay engine mismatch",
        requested: options.engine ?? REPLAY_ENGINE_VERSION,
        actual: parsed.engineVersion ?? REPLAY_ENGINE_VERSION,
      },
      { status: 409, headers: { "Cache-Control": PRIVATE_NO_STORE } },
    );
  }
  const cacheControl = options.sealed
    ? SEALED_REPLAY_CACHE_CONTROL
    : TRAILING_REPLAY_CACHE_CONTROL;
  return jsonPublic(options.request, parsed, cacheControl);
}
