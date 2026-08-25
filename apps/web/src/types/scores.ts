export type MatchStatus = "live" | "upcoming" | "final";

export type ScoreSide = {
  name: string;
  fullName?: string;
  shortName: string;
  photoUrl?: string | null;
  sets: number[];
  winner?: boolean;
};

export type ScoreCard = {
  id: string;
  status: MatchStatus;
  circuitRank: number;
  tournament: string;
  round: string;
  startLabel?: string;
  href: string;
  home: ScoreSide;
  away: ScoreSide;
};

export type ScoresFeed = {
  updatedAt: string;
  items: ScoreCard[];
};
