import { upstreamHeaders } from "@/lib/request-id";

export type UpstreamGender = "MALE" | "FEMALE";
export type UpstreamRankingType = "SINGLES" | "DOUBLES";

export type UpstreamRanking = {
  id: string;
  playerId: string;
  playerName: string;
  rank: number;
  points: number;
  rankingDate?: string;
  rankingType?: UpstreamRankingType;
  gender?: UpstreamGender;
};

export type UpstreamPlayer = {
  id: string;
  externalId?: string | null;
  firstName: string;
  lastName: string;
  nationality?: string | null;
  currentRanking?: number | null;
  currentPoints?: number | null;
  gender?: UpstreamGender;
};

function tennisDataBase(): string {
  return (process.env.TENNIS_DATA_SERVICE_URL ?? "http://localhost:8083").replace(/\/$/, "");
}

export class TennisDataUpstreamError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "TennisDataUpstreamError";
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new TennisDataUpstreamError(`tennis-data-service ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

export async function fetchUpstreamRankings(options: {
  gender: UpstreamGender;
  type?: UpstreamRankingType;
}): Promise<UpstreamRanking[]> {
  const params = new URLSearchParams({
    gender: options.gender,
    type: options.type ?? "SINGLES",
  });
  let response: Response;
  try {
    response = await fetch(`${tennisDataBase()}/api/tennis/rankings?${params}`, {
      headers: upstreamHeaders(),
      cache: "no-store",
    });
  } catch {
    throw new TennisDataUpstreamError("tennis-data-service unreachable", 502);
  }
  return readJson<UpstreamRanking[]>(response);
}

export async function fetchUpstreamPlayers(options?: {
  gender?: UpstreamGender;
}): Promise<UpstreamPlayer[]> {
  const params = new URLSearchParams();
  if (options?.gender) params.set("gender", options.gender);
  const query = params.toString();
  let response: Response;
  try {
    response = await fetch(
      `${tennisDataBase()}/api/tennis/players${query ? `?${query}` : ""}`,
      {
        headers: upstreamHeaders(),
        cache: "no-store",
      },
    );
  } catch {
    throw new TennisDataUpstreamError("tennis-data-service unreachable", 502);
  }
  return readJson<UpstreamPlayer[]>(response);
}
