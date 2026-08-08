import type {
  CompareAnalytics,
  CreateSavedViewPayload,
  MatchAnalytics,
  MatchReport,
  PlayerAnalytics,
  PlayerAnalyticsQuery,
  PlayerTrends,
  SavedAnalyticsView,
  TournamentAnalytics,
  UpdateSavedViewPayload,
} from "@/types/analytics";

function analyticsServiceBase(): string {
  return (process.env.ANALYTICS_SERVICE_URL ?? "http://localhost:18086").replace(/\/$/, "");
}

export class AnalyticsUpstreamError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "AnalyticsUpstreamError";
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new AnalyticsUpstreamError(`analytics-service ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

async function readText(response: Response): Promise<string> {
  if (!response.ok) {
    throw new AnalyticsUpstreamError(`analytics-service ${response.status}`, response.status);
  }
  return response.text();
}

function authHeaders(token: string | null, userId: string): HeadersInit {
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
    "X-User-Id": userId,
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function upstreamFetch(path: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(`${analyticsServiceBase()}${path}`, {
      cache: "no-store",
      ...init,
    });
  } catch {
    throw new AnalyticsUpstreamError("analytics-service unreachable", 502);
  }
}

function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export async function fetchUpstreamPlayerAnalytics(
  playerId: string,
  query?: PlayerAnalyticsQuery,
): Promise<PlayerAnalytics> {
  const qs = queryString({
    from: query?.from,
    to: query?.to,
    surface: query?.surface && query.surface !== "ALL" ? query.surface : undefined,
    page: query?.page,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/analytics/players/${playerId}${qs}`, {
    headers: { Accept: "application/json" },
  });
  return readJson<PlayerAnalytics>(response);
}

export async function fetchUpstreamPlayerTrends(
  playerId: string,
  query?: Pick<PlayerAnalyticsQuery, "from" | "to" | "surface"> & { size?: number },
): Promise<PlayerTrends> {
  const qs = queryString({
    from: query?.from,
    to: query?.to,
    surface: query?.surface && query.surface !== "ALL" ? query.surface : undefined,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/analytics/players/${playerId}/trends${qs}`, {
    headers: { Accept: "application/json" },
  });
  return readJson<PlayerTrends>(response);
}

export async function fetchUpstreamCompare(
  playerA: string,
  playerB: string,
  query?: Pick<PlayerAnalyticsQuery, "from" | "to">,
): Promise<CompareAnalytics> {
  const qs = queryString({
    playerA,
    playerB,
    from: query?.from,
    to: query?.to,
  });
  const response = await upstreamFetch(`/api/analytics/compare${qs}`, {
    headers: { Accept: "application/json" },
  });
  return readJson<CompareAnalytics>(response);
}

export async function fetchUpstreamMatchAnalytics(matchId: string): Promise<MatchAnalytics> {
  const response = await upstreamFetch(`/api/analytics/matches/${matchId}`, {
    headers: { Accept: "application/json" },
  });
  return readJson<MatchAnalytics>(response);
}

export async function fetchUpstreamMatchReport(matchId: string): Promise<MatchReport> {
  const response = await upstreamFetch(`/api/analytics/matches/${matchId}/report`, {
    headers: { Accept: "application/json" },
  });
  return readJson<MatchReport>(response);
}

export async function fetchUpstreamTournamentAnalytics(
  tournamentKey: string,
): Promise<TournamentAnalytics> {
  const response = await upstreamFetch(
    `/api/analytics/tournaments/${encodeURIComponent(tournamentKey)}`,
    { headers: { Accept: "application/json" } },
  );
  return readJson<TournamentAnalytics>(response);
}

export async function fetchUpstreamPlayerCsv(
  playerId: string,
  query?: Pick<PlayerAnalyticsQuery, "from" | "to" | "surface">,
): Promise<string> {
  const qs = queryString({
    from: query?.from,
    to: query?.to,
    surface: query?.surface && query.surface !== "ALL" ? query.surface : undefined,
  });
  const response = await upstreamFetch(`/api/analytics/players/${playerId}/export.csv${qs}`, {
    headers: { Accept: "text/csv" },
  });
  return readText(response);
}

export async function fetchUpstreamMatchCsv(matchId: string): Promise<string> {
  const response = await upstreamFetch(`/api/analytics/matches/${matchId}/export.csv`, {
    headers: { Accept: "text/csv" },
  });
  return readText(response);
}

export async function fetchUpstreamSavedViews(
  token: string | null,
  userId: string,
): Promise<SavedAnalyticsView[]> {
  const response = await upstreamFetch("/api/analytics/views", {
    headers: authHeaders(token, userId),
  });
  return readJson<SavedAnalyticsView[]>(response);
}

export async function fetchUpstreamSavedView(
  token: string | null,
  userId: string,
  id: string,
): Promise<SavedAnalyticsView> {
  const response = await upstreamFetch(`/api/analytics/views/${id}`, {
    headers: authHeaders(token, userId),
  });
  return readJson<SavedAnalyticsView>(response);
}

export async function createUpstreamSavedView(
  token: string | null,
  userId: string,
  payload: CreateSavedViewPayload,
): Promise<SavedAnalyticsView> {
  const response = await upstreamFetch("/api/analytics/views", {
    method: "POST",
    headers: authHeaders(token, userId),
    body: JSON.stringify(payload),
  });
  return readJson<SavedAnalyticsView>(response);
}

export async function updateUpstreamSavedView(
  token: string | null,
  userId: string,
  id: string,
  payload: UpdateSavedViewPayload,
): Promise<SavedAnalyticsView> {
  const response = await upstreamFetch(`/api/analytics/views/${id}`, {
    method: "PUT",
    headers: authHeaders(token, userId),
    body: JSON.stringify(payload),
  });
  return readJson<SavedAnalyticsView>(response);
}

export async function deleteUpstreamSavedView(
  token: string | null,
  userId: string,
  id: string,
): Promise<void> {
  const response = await upstreamFetch(`/api/analytics/views/${id}`, {
    method: "DELETE",
    headers: authHeaders(token, userId),
  });
  if (!response.ok && response.status !== 204) {
    throw new AnalyticsUpstreamError(`analytics-service ${response.status}`, response.status);
  }
}

export async function favoriteUpstreamSavedView(
  token: string | null,
  userId: string,
  id: string,
  favorite: boolean,
): Promise<SavedAnalyticsView> {
  const response = await upstreamFetch(
    `/api/analytics/views/${id}/favorite?favorite=${favorite}`,
    {
      method: "PATCH",
      headers: authHeaders(token, userId),
      body: JSON.stringify({ favorite }),
    },
  );
  return readJson<SavedAnalyticsView>(response);
}
