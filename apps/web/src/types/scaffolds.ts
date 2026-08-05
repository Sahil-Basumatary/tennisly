import type { MatchStatus, ScoreCard } from "@/types/scores";

export type ScoreboardFilter =
  | "all"
  | "mens_singles"
  | "womens_singles"
  | "mens_doubles"
  | "womens_doubles";

export type ScoreboardGroup = {
  tournamentId: string;
  tournamentName: string;
  location: string;
  matches: ScoreCard[];
};

export type ScoreboardDay = {
  dateLabel: string;
  filters: { id: ScoreboardFilter; label: string }[];
  groups: ScoreboardGroup[];
};

export type PlayerRow = {
  id: string;
  rank: number;
  name: string;
  country: string;
  points: number;
  href: string;
};

export type PlayersBoard = {
  tour: "atp" | "wta";
  updatedAt: string;
  rows: PlayerRow[];
};

export type StandingRow = {
  position: number;
  player: string;
  points: number;
};

export type FixtureRow = {
  id: string;
  status: MatchStatus;
  startLabel: string;
  round: string;
  home: string;
  away: string;
  href: string;
};

export type TournamentBoard = {
  name: string;
  surface: string;
  location: string;
  standings: StandingRow[];
  fixtures: FixtureRow[];
};

export type MatchCentrePanel = {
  id: string;
  status: MatchStatus;
  tournament: string;
  round: string;
  court: string;
  home: { id: string; name: string; country: string; seed?: number };
  away: { id: string; name: string; country: string; seed?: number };
  score: {
    homeSets: number[];
    awaySets: number[];
    homeGames: number;
    awayGames: number;
    homePoints: string;
    awayPoints: string;
    server: "HOME" | "AWAY";
  };
  stats: { label: string; home: string; away: string }[];
};
