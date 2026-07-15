import type {
  MatchCentrePanel,
  PlayersBoard,
  ScoreboardDay,
  TournamentBoard,
} from "@/types/scaffolds";
import { getScoresFeed } from "@/services/scores";

export async function getScoreboardDay(): Promise<ScoreboardDay> {
  const feed = await getScoresFeed();
  return {
    dateLabel: "Tuesday, July 14, 2026",
    filters: [
      { id: "all", label: "All matches" },
      { id: "mens_singles", label: "Men's Singles" },
      { id: "womens_singles", label: "Women's Singles" },
      { id: "mens_doubles", label: "Men's Doubles" },
      { id: "womens_doubles", label: "Women's Doubles" },
    ],
    groups: [
      {
        tournamentId: "masters-1000",
        tournamentName: "ATP Masters 1000",
        location: "Centre courts",
        matches: feed.items.filter((m) => m.tournament === "ATP Masters 1000"),
      },
      {
        tournamentId: "wta-1000",
        tournamentName: "WTA 1000",
        location: "Show courts",
        matches: feed.items.filter((m) => m.tournament === "WTA 1000"),
      },
      {
        tournamentId: "atp-500",
        tournamentName: "ATP 500",
        location: "Outdoor hard",
        matches: feed.items.filter((m) => m.tournament === "ATP 500"),
      },
    ].filter((g) => g.matches.length > 0),
  };
}

export async function getPlayersBoard(): Promise<PlayersBoard> {
  return {
    tour: "atp",
    updatedAt: new Date().toISOString(),
    rows: [
      {
        id: "p-sinner",
        rank: 1,
        name: "Jannik Sinner",
        country: "ITA",
        points: 11830,
        href: "/players#p-sinner",
      },
      {
        id: "p-alcaraz",
        rank: 2,
        name: "Carlos Alcaraz",
        country: "ESP",
        points: 11250,
        href: "/players#p-alcaraz",
      },
      {
        id: "p-djokovic",
        rank: 3,
        name: "Novak Djokovic",
        country: "SRB",
        points: 7840,
        href: "/players#p-djokovic",
      },
      {
        id: "p-zverev",
        rank: 4,
        name: "Alexander Zverev",
        country: "GER",
        points: 6915,
        href: "/players#p-zverev",
      },
      {
        id: "p-medvedev",
        rank: 5,
        name: "Daniil Medvedev",
        country: "RUS",
        points: 5930,
        href: "/players#p-medvedev",
      },
    ],
  };
}

export async function getTournamentBoard(): Promise<TournamentBoard> {
  return {
    name: "Masters 1000 — Season Standings",
    surface: "Hard",
    location: "Tour-wide",
    standings: [
      { position: 1, player: "J. Sinner", played: 12, won: 11, lost: 1, points: 2000 },
      { position: 2, player: "C. Alcaraz", played: 11, won: 9, lost: 2, points: 1800 },
      { position: 3, player: "N. Djokovic", played: 10, won: 8, lost: 2, points: 1200 },
      { position: 4, player: "A. Zverev", played: 12, won: 8, lost: 4, points: 900 },
      { position: 5, player: "D. Medvedev", played: 11, won: 7, lost: 4, points: 720 },
    ],
    fixtures: [
      {
        id: "m-alcaraz-sinner",
        status: "live",
        startLabel: "Live",
        round: "SF",
        home: "C. Alcaraz",
        away: "J. Sinner",
        href: "/matches/m-alcaraz-sinner",
      },
      {
        id: "m-djokovic-medvedev",
        status: "upcoming",
        startLabel: "19:30",
        round: "SF",
        home: "N. Djokovic",
        away: "D. Medvedev",
        href: "/matches/m-djokovic-medvedev",
      },
      {
        id: "m-zverev-ruud",
        status: "final",
        startLabel: "Final",
        round: "F",
        home: "A. Zverev",
        away: "C. Ruud",
        href: "/matches/m-zverev-ruud",
      },
    ],
  };
}

export async function getMatchCentre(
  id: string,
): Promise<MatchCentrePanel | null> {
  const feed = await getScoresFeed();
  const card = feed.items.find((m) => m.id === id);
  if (!card) return null;
  return {
    id: card.id,
    status: card.status,
    tournament: card.tournament,
    round: card.round,
    court: "Centre Court",
    home: {
      name: card.home.name,
      country: "ESP",
      seed: 2,
    },
    away: {
      name: card.away.name,
      country: "ITA",
      seed: 1,
    },
    score: {
      homeSets: card.home.sets,
      awaySets: card.away.sets,
    },
    stats: [
      { label: "Aces", home: "8", away: "11" },
      { label: "Double faults", home: "2", away: "1" },
      { label: "1st serve %", home: "68%", away: "71%" },
      { label: "Break points won", home: "3/7", away: "4/9" },
      { label: "Winners", home: "24", away: "29" },
      { label: "Unforced errors", home: "18", away: "15" },
    ],
  };
}
