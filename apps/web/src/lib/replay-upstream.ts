import { NextResponse } from "next/server";

export function replayServiceBase(): string {
  return (process.env.REPLAY_SERVICE_URL ?? "http://localhost:8085").replace(/\/$/, "");
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
