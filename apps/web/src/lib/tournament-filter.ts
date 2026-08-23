import type { UpstreamMatch } from "@/types/match-catalogue";

export type TournamentQuery = {
  tour?: string;
  level?: string;
  name?: string;
};

const SLAM = /australian open|roland garros|french open|wimbledon|us open|u\.s\. open/;
const ATP_ONLY = /\batp\b|masters 1000|united cup|next gen|davis/;
const WTA_ONLY = /\bwta\b|billie jean|bjk|fed cup/;
const DAVIS = /davis/;
const BJK = /billie jean|bjk|fed cup/;

function haystack(match: UpstreamMatch): string {
  const meta = match.metadata ?? {};
  return [
    meta.tournamentName,
    meta.tournamentShortName,
    meta.tour,
    meta.circuit,
    meta.competition,
  ]
    .filter((value): value is string => typeof value === "string")
    .join(" ")
    .toLowerCase();
}

export function parseTournamentQuery(params: {
  tour?: string;
  level?: string;
  name?: string;
}): TournamentQuery {
  return {
    tour: params.tour === "wta" || params.tour === "atp" ? params.tour : undefined,
    level: params.level === "grand_slam" ? "grand_slam" : undefined,
    name: params.name === "davis" || params.name === "bjk" ? params.name : undefined,
  };
}

export function filterMatchesForTournament(
  matches: UpstreamMatch[],
  query: TournamentQuery,
): UpstreamMatch[] {
  if (!query.tour && !query.level && !query.name) return matches;
  return matches.filter((match) => {
    const text = haystack(match);
    if (query.level === "grand_slam" && !SLAM.test(text)) return false;
    if (query.name === "davis" && !DAVIS.test(text)) return false;
    if (query.name === "bjk" && !BJK.test(text)) return false;
    if (query.tour === "atp" && WTA_ONLY.test(text) && !SLAM.test(text)) return false;
    if (query.tour === "wta" && ATP_ONLY.test(text) && !SLAM.test(text)) return false;
    return true;
  });
}

export type TournamentHeading = {
  name: string;
  location: string;
  standingsLabel: string;
};

export function tournamentHeading(query: TournamentQuery): TournamentHeading {
  if (query.name === "davis") {
    return { name: "Davis Cup", location: "National teams", standingsLabel: "ATP rankings" };
  }
  if (query.name === "bjk") {
    return {
      name: "Billie Jean King Cup",
      location: "National teams",
      standingsLabel: "WTA rankings",
    };
  }
  if (query.level === "grand_slam") {
    return { name: "Grand Slams", location: "Majors", standingsLabel: "Rankings" };
  }
  if (query.tour === "wta") {
    return { name: "WTA Tour", location: "Women's tour", standingsLabel: "WTA rankings" };
  }
  if (query.tour === "atp") {
    return { name: "ATP Tour", location: "Men's tour", standingsLabel: "ATP rankings" };
  }
  return { name: "All competitions", location: "Tour", standingsLabel: "Rankings" };
}

export function tournamentActiveId(query: TournamentQuery): string {
  if (query.name === "davis") return "davis";
  if (query.name === "bjk") return "bjk";
  if (query.level === "grand_slam") return "slams";
  if (query.tour === "wta") return "wta";
  if (query.tour === "atp") return "atp";
  return "overview";
}

export function standingsGender(query: TournamentQuery): "MALE" | "FEMALE" {
  if (query.tour === "wta" || query.name === "bjk") return "FEMALE";
  return "MALE";
}

export function surfaceLabel(surface?: string): string {
  if (!surface) return "—";
  return surface.charAt(0) + surface.slice(1).toLowerCase();
}

export type CircuitId = "slams" | "atp" | "wta" | "davis" | "bjk";

export function classifyTournamentName(name: string): CircuitId | null {
  const text = name.toLowerCase();
  if (SLAM.test(text)) return "slams";
  if (DAVIS.test(text)) return "davis";
  if (BJK.test(text)) return "bjk";
  if (WTA_ONLY.test(text)) return "wta";
  if (ATP_ONLY.test(text)) return "atp";
  return null;
}
