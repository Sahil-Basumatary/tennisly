import {
  getMatchCentre as getCatalogueMatchCentre,
  getScoreboardDay as getCatalogueScoreboardDay,
  getTournamentBoard as getCatalogueTournamentBoard,
} from "@/services/catalogue";
import type {
  MatchCentrePanel,
  PlayersBoard,
  ScoreboardDay,
  TournamentBoard,
} from "@/types/scaffolds";

export async function getScoreboardDay(uiStatus?: string): Promise<ScoreboardDay> {
  return getCatalogueScoreboardDay(uiStatus);
}

export async function getMatchCentre(id: string): Promise<MatchCentrePanel | null> {
  return getCatalogueMatchCentre(id);
}

export async function getTournamentBoard(): Promise<TournamentBoard> {
  return getCatalogueTournamentBoard();
}

/** Rankings stay on tennis-data until that BFF lands — keep a thin live-shaped board. */
export async function getPlayersBoard(): Promise<PlayersBoard> {
  return {
    tour: "atp",
    updatedAt: new Date().toISOString(),
    rows: [
      {
        id: "b1000000-0000-4000-8000-000000000002",
        rank: 1,
        name: "Jannik Sinner",
        country: "ITA",
        points: 11830,
        href: "/players#sinner",
      },
      {
        id: "b1000000-0000-4000-8000-000000000001",
        rank: 2,
        name: "Carlos Alcaraz",
        country: "ESP",
        points: 11250,
        href: "/players#alcaraz",
      },
      {
        id: "b1000000-0000-4000-8000-000000000003",
        rank: 3,
        name: "Novak Djokovic",
        country: "SRB",
        points: 7840,
        href: "/players#djokovic",
      },
      {
        id: "b1000000-0000-4000-8000-000000000005",
        rank: 4,
        name: "Alexander Zverev",
        country: "GER",
        points: 6915,
        href: "/players#zverev",
      },
      {
        id: "b1000000-0000-4000-8000-000000000004",
        rank: 5,
        name: "Daniil Medvedev",
        country: "RUS",
        points: 5930,
        href: "/players#medvedev",
      },
    ],
  };
}
