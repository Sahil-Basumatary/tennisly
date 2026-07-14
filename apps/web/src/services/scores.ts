import type { ScoresFeed } from "@/types/scores";

/** Mock feed shaped like match-service summaries for later live wiring. */
export async function getScoresFeed(): Promise<ScoresFeed> {
  return {
    updatedAt: new Date().toISOString(),
    items: [
      {
        id: "m-alcaraz-sinner",
        status: "live",
        tournament: "ATP Masters 1000",
        round: "SF",
        href: "/matches/m-alcaraz-sinner",
        home: {
          name: "C. Alcaraz",
          shortName: "ALC",
          sets: [6, 3, 4],
        },
        away: {
          name: "J. Sinner",
          shortName: "SIN",
          sets: [4, 6, 5],
        },
      },
      {
        id: "m-swiatek-gauff",
        status: "live",
        tournament: "WTA 1000",
        round: "QF",
        href: "/matches/m-swiatek-gauff",
        home: {
          name: "I. Swiatek",
          shortName: "SWI",
          sets: [6, 2],
        },
        away: {
          name: "C. Gauff",
          shortName: "GAU",
          sets: [3, 1],
        },
      },
      {
        id: "m-djokovic-medvedev",
        status: "upcoming",
        tournament: "ATP Masters 1000",
        round: "SF",
        startLabel: "7:30 PM",
        href: "/matches/m-djokovic-medvedev",
        home: {
          name: "N. Djokovic",
          shortName: "DJO",
          sets: [],
        },
        away: {
          name: "D. Medvedev",
          shortName: "MED",
          sets: [],
        },
      },
      {
        id: "m-sabalenka-rybakina",
        status: "final",
        tournament: "WTA 1000",
        round: "QF",
        href: "/matches/m-sabalenka-rybakina",
        home: {
          name: "A. Sabalenka",
          shortName: "SAB",
          sets: [7, 6],
          winner: true,
        },
        away: {
          name: "E. Rybakina",
          shortName: "RYB",
          sets: [6, 4],
        },
      },
      {
        id: "m-zverev-ruud",
        status: "final",
        tournament: "ATP 500",
        round: "F",
        href: "/matches/m-zverev-ruud",
        home: {
          name: "A. Zverev",
          shortName: "ZVE",
          sets: [6, 3, 6],
          winner: true,
        },
        away: {
          name: "C. Ruud",
          shortName: "RUU",
          sets: [4, 6, 3],
        },
      },
    ],
  };
}
