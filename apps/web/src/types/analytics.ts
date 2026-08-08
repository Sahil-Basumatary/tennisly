export type TapeSideMetrics = {
  pointsWon: number;
  servicePointsWon: number;
  breakPointsWon: number;
};

export type SurfaceSummary = {
  matchesPlayed: number;
  wins: number;
  losses: number;
  pointsWon: number;
  servicePointsWon: number;
  breakPointsWon: number;
};

export type PlayerSummary = {
  matchesPlayed: number;
  wins: number;
  losses: number;
  pointsWon: number;
  servicePointsWon: number;
  breakPointsWon: number;
  bySurface: Record<string, SurfaceSummary>;
};

export type PlayerMatchRow = {
  matchId: string;
  opponentId: string;
  opponentName: string;
  won: boolean | null;
  surface: string;
  tournamentName: string;
  season: number | null;
  endedAt: string | null;
  scheduledAt: string | null;
  metrics: TapeSideMetrics;
};

export type PlayerAnalytics = {
  playerId: string;
  summary: PlayerSummary;
  matches: PlayerMatchRow[];
  page: number;
  size: number;
  totalMatches: number;
};

export type PlayerTrendPoint = {
  matchId: string;
  endedAt: string | null;
  scheduledAt: string | null;
  metrics: TapeSideMetrics;
  won: boolean | null;
};

export type PlayerTrends = {
  playerId: string;
  trends: PlayerTrendPoint[];
};

export type HeadToHeadMeeting = {
  matchId: string;
  endedAt: string | null;
  scheduledAt: string | null;
  playerAWon: boolean | null;
  surface: string;
  tournamentName: string;
  playerAMetrics: TapeSideMetrics;
  playerBMetrics: TapeSideMetrics;
};

export type CompareAnalytics = {
  meetingCount: number;
  aWins: number;
  bWins: number;
  unknownResults: number;
  meetings: HeadToHeadMeeting[];
  playerA: TapeSideMetrics;
  playerB: TapeSideMetrics;
};

export type MatchAnalytics = {
  matchId: string;
  externalId: string | null;
  tournamentId: string | null;
  tournamentKey: string | null;
  tournamentName: string | null;
  season: number | null;
  surface: string | null;
  status: string | null;
  bestOfSets: number;
  scheduledAt: string | null;
  startedAt: string | null;
  endedAt: string | null;
  homePlayerId: string;
  homeDisplayName: string;
  awayPlayerId: string;
  awayDisplayName: string;
  winnerPlayerId: string | null;
  homeMetrics: TapeSideMetrics;
  awayMetrics: TapeSideMetrics;
  pointsPlayed: number;
  scoreSnapshot: Record<string, unknown> | null;
  indexedAt: string | null;
};

export type TournamentTopPlayer = {
  playerId: string;
  displayName: string;
  pointsWon: number;
};

export type TournamentAnalytics = {
  tournamentKey: string;
  tournamentName: string | null;
  season: number | null;
  matchCount: number;
  surfaceBreakdown: Record<string, number>;
  topPlayers: TournamentTopPlayer[];
};

export type MatchReportSection = {
  id: string;
  label: string;
  content: unknown;
};

export type MatchReport = {
  generatedAt: string;
  title: string;
  sections: MatchReportSection[];
  match: MatchAnalytics;
};

export type SavedAnalyticsView = {
  id: string;
  name: string;
  favorite: boolean;
  config: Record<string, unknown>;
  version: number;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateSavedViewPayload = {
  name: string;
  config: Record<string, unknown>;
  favorite?: boolean;
};

export type UpdateSavedViewPayload = {
  name: string;
  config: Record<string, unknown>;
  favorite?: boolean;
  version: number;
};

export type AnalyticsSurfaceFilter = "ALL" | "HARD" | "CLAY" | "GRASS";

export type PlayerAnalyticsQuery = {
  from?: string;
  to?: string;
  surface?: string;
  page?: number;
  size?: number;
};
