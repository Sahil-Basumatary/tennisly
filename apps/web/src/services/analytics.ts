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

class AnalyticsClientError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "AnalyticsClientError";
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new AnalyticsClientError(body?.error ?? `analytics ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export async function getPlayerAnalytics(
  playerId: string,
  query?: PlayerAnalyticsQuery,
): Promise<PlayerAnalytics> {
  const qs = queryString({
    from: query?.from,
    to: query?.to,
    surface: query?.surface,
    page: query?.page,
    size: query?.size,
  });
  const response = await fetch(`/api/analytics/players/${playerId}${qs}`, {
    cache: "no-store",
  });
  return readJson<PlayerAnalytics>(response);
}

export async function getPlayerTrends(
  playerId: string,
  query?: Pick<PlayerAnalyticsQuery, "from" | "to" | "surface"> & { size?: number },
): Promise<PlayerTrends> {
  const qs = queryString({
    from: query?.from,
    to: query?.to,
    surface: query?.surface,
    size: query?.size,
  });
  const response = await fetch(`/api/analytics/players/${playerId}/trends${qs}`, {
    cache: "no-store",
  });
  return readJson<PlayerTrends>(response);
}

export async function getCompareAnalytics(
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
  const response = await fetch(`/api/analytics/compare${qs}`, { cache: "no-store" });
  return readJson<CompareAnalytics>(response);
}

export async function getMatchAnalytics(matchId: string): Promise<MatchAnalytics> {
  const response = await fetch(`/api/analytics/matches/${matchId}`, { cache: "no-store" });
  return readJson<MatchAnalytics>(response);
}

export async function getMatchReport(matchId: string): Promise<MatchReport> {
  const response = await fetch(`/api/analytics/matches/${matchId}/report`, { cache: "no-store" });
  return readJson<MatchReport>(response);
}

export async function getTournamentAnalytics(tournamentKey: string): Promise<TournamentAnalytics> {
  const response = await fetch(
    `/api/analytics/tournaments/${encodeURIComponent(tournamentKey)}`,
    { cache: "no-store" },
  );
  return readJson<TournamentAnalytics>(response);
}

export async function listSavedViews(): Promise<SavedAnalyticsView[]> {
  const response = await fetch("/api/analytics/views", { cache: "no-store" });
  return readJson<SavedAnalyticsView[]>(response);
}

export async function createSavedView(payload: CreateSavedViewPayload): Promise<SavedAnalyticsView> {
  const response = await fetch("/api/analytics/views", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return readJson<SavedAnalyticsView>(response);
}

export async function updateSavedView(
  id: string,
  payload: UpdateSavedViewPayload,
): Promise<SavedAnalyticsView> {
  const response = await fetch(`/api/analytics/views/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return readJson<SavedAnalyticsView>(response);
}

export async function deleteSavedView(id: string): Promise<void> {
  const response = await fetch(`/api/analytics/views/${id}`, { method: "DELETE" });
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new AnalyticsClientError(body?.error ?? `analytics ${response.status}`, response.status);
  }
}

export async function setSavedViewFavorite(id: string, favorite: boolean): Promise<SavedAnalyticsView> {
  const response = await fetch(`/api/analytics/views/${id}/favorite`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ favorite }),
  });
  return readJson<SavedAnalyticsView>(response);
}

export { AnalyticsClientError };
