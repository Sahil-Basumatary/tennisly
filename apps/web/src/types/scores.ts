export type MatchStatus = "live" | "upcoming" | "final";

export type ScoreSide = {
  name: string;
  shortName: string;
  photoUrl?: string | null;
  sets: number[];
  winner?: boolean;
};

export type ScoreCard = {
  id: string;
  status: MatchStatus;
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
