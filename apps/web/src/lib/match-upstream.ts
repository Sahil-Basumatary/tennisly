import { isReplayMatchUuid } from "@/lib/replay-index";
import { upstreamHeaders } from "@/lib/request-id";
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
