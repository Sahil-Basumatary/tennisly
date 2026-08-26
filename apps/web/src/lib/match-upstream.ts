import { isReplayMatchUuid } from "@/lib/replay-index";
import { upstreamHeaders } from "@/lib/request-id";
import type { LiveCursorDocument, LiveScoreDocument } from "@/lib/live-score-document";
import { toLiveCursorDocument, toLiveScoreDocument } from "@/lib/live-score-document";
import type { UpstreamMatchPoint } from "@/lib/match-stats";
import type { UpstreamMatch, UpstreamMatchStatus } from "@/types/match-catalogue";

function matchServiceBase(): string {
  return (process.env.MATCH_SERVICE_URL ?? "http://localhost:8084").replace(/\/$/, "");
}

export class MatchUpstreamError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "MatchUpstreamError";
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new MatchUpstreamError(`match-service ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

export async function fetchUpstreamMatches(options?: {
  status?: UpstreamMatchStatus;
  tournamentId?: string;
  page?: number;
  size?: number;
}): Promise<UpstreamMatch[]> {
  const params = new URLSearchParams();
  if (options?.status) params.set("status", options.status);
  if (options?.tournamentId) params.set("tournamentId", options.tournamentId);
  if (options?.page != null) params.set("page", String(options.page));
  if (options?.size != null) params.set("size", String(options.size));
  const query = params.toString();
  const url = `${matchServiceBase()}/api/matches${query ? `?${query}` : ""}`;
  let response: Response;
  try {
    response = await fetch(url, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    throw new MatchUpstreamError("match-service unreachable", 502);
  }
  return readJson<UpstreamMatch[]>(response);
}

/** Resolve a route id that may be a UUID or a broadcast externalId. */
export async function fetchUpstreamMatch(idOrExternal: string): Promise<UpstreamMatch | null> {
  const base = matchServiceBase();
  const path = isReplayMatchUuid(idOrExternal)
    ? `${base}/api/matches/${idOrExternal}`
    : `${base}/api/matches/external/${encodeURIComponent(idOrExternal)}`;
  let response: Response;
  try {
    response = await fetch(path, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    throw new MatchUpstreamError("match-service unreachable", 502);
  }
  if (response.status === 404) return null;
  return readJson<UpstreamMatch>(response);
}

export async function fetchUpstreamMatchPoints(matchId: string): Promise<UpstreamMatchPoint[]> {
  let response: Response;
  try {
    response = await fetch(`${matchServiceBase()}/api/matches/${matchId}/points`, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    throw new MatchUpstreamError("match-service unreachable", 502);
  }
  if (response.status === 404) return [];
  return readJson<UpstreamMatchPoint[]>(response);
}

export type UpstreamMatchEvent = {
  id: string;
  sequence: number;
  eventType: string;
  payload?: Record<string, unknown>;
  createdAt?: string;
};

export async function fetchUpstreamTicker(): Promise<UpstreamMatch[] | null> {
  let response: Response;
  try {
    response = await fetch(`${matchServiceBase()}/api/matches/ticker`, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    throw new MatchUpstreamError("match-service unreachable", 502);
  }
  if (response.status === 404) return null;
  return readJson<UpstreamMatch[]>(response);
}

async function fetchCompactJson<T>(path: string): Promise<T | null | undefined> {
  let response: Response;
  try {
    response = await fetch(`${matchServiceBase()}${path}`, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    return undefined;
  }
  if (response.status === 404) return null;
  if (!response.ok) return undefined;
  return (await response.json()) as T;
}

export async function fetchUpstreamLiveScore(idOrExternal: string): Promise<LiveScoreDocument | null> {
  if (isReplayMatchUuid(idOrExternal)) {
    const compact = await fetchCompactJson<LiveScoreDocument>(
      `/api/matches/${idOrExternal}/live`,
    );
    if (compact) return compact;
  }
  const match = await fetchUpstreamMatch(idOrExternal);
  return match ? toLiveScoreDocument(match) : null;
}

export async function fetchUpstreamLiveCursor(idOrExternal: string): Promise<LiveCursorDocument | null> {
  if (isReplayMatchUuid(idOrExternal)) {
    const compact = await fetchCompactJson<LiveCursorDocument>(
      `/api/matches/${idOrExternal}/cursor`,
    );
    if (compact) return compact;
  }
  const live = await fetchUpstreamLiveScore(idOrExternal);
  return live ? toLiveCursorDocument(live) : null;
}

export async function fetchUpstreamMatchEvents(
  matchId: string,
  afterSequence = 0,
  limit = 100,
): Promise<UpstreamMatchEvent[]> {
  const params = new URLSearchParams();
  params.set("afterSequence", String(Math.max(0, afterSequence)));
  params.set("limit", String(Math.max(1, Math.min(limit, 1_000))));
  let response: Response;
  try {
    response = await fetch(
      `${matchServiceBase()}/api/matches/${matchId}/events?${params}`,
      {
        headers: upstreamHeaders(),
        cache: "no-store",
      },
    );
  } catch {
    throw new MatchUpstreamError("match-service unreachable", 502);
  }
  if (response.status === 404) return [];
  return readJson<UpstreamMatchEvent[]>(response);
}
