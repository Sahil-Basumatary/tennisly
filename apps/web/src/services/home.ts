import { classifyTournamentName, type CircuitId } from "@/lib/tournament-filter";
import { rankHomeReplayCandidates, type HomeReplayFeature } from "@/lib/home-replay";
import { toMatchCentrePanel } from "@/lib/match-mapper";
import { matchCircuitRank } from "@/lib/match-order";
import { fetchUpstreamMatch, fetchUpstreamMatches } from "@/lib/match-upstream";
import { headshotByName } from "@/lib/player-photos";
import { isReplayPointReady } from "@/lib/replay-upstream";
import { fetchWikiPlayerMediaMap, type WikiPlayerMedia } from "@/lib/wikipedia-upstream";
import { getPlayersBoard } from "@/services/catalogue";
import { getScoresFeed } from "@/services/scores";
import { matchStoryHref, playerStoryHref } from "@/types/editorial";
import type { PlayerRow } from "@/types/scaffolds";
import type { MatchStatus, ScoreCard } from "@/types/scores";

export type StoryTag = "On court" | "Player profile" | "Live";

export type HomeStory = {
  id: string;
  tag: StoryTag;
  title: string;
  href: string;
  imageSrc: string | null;
  imageAlt: string;
  imageCredit: string | null;
  summary: string | null;
  publishedLabel: string;
};

export type TourCircuit = {
  id: CircuitId;
  label: string;
  href: string;
  liveCount: number;
  upcomingCount: number;
  sample: string | null;
};

export type TourPulse = {
  featured: {
    href: string;
    title: string;
    meta: string;
    status: MatchStatus;
  };
  circuits: TourCircuit[];
};

export type HomeMoreItem = {
  id: string;
  href: string;
  eyebrow: string;
  title: string;
  meta: string;
};

export type HomeContent = {
  hero: {
    headline: string;
    ctaLabel: string;
    ctaHref: string;
    imageSrc: string;
    imageAlt: string;
  };
  tourPulse: TourPulse | null;
  onCourt: HomeStory[];
  onCourtMore: HomeMoreItem[];
  featured: {
    eyebrow: string;
    headline: string;
    label: string;
    href: string;
    imageSrc: string;
    imageAlt: string;
  } | null;
  playerProfiles: HomeStory[];
  empty: boolean;
  replay: HomeReplayFeature | null;
};

const IMG = {
  grassAction:
    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=2400&q=80",
} as const;

const CIRCUITS: { id: CircuitId; label: string; href: string }[] = [
  { id: "slams", label: "Grand Slams", href: "/tournaments?level=grand_slam" },
  { id: "atp", label: "ATP Tour", href: "/tournaments?tour=atp" },
  { id: "wta", label: "WTA Tour", href: "/tournaments?tour=wta" },
  { id: "davis", label: "Davis Cup", href: "/tournaments?name=davis" },
  { id: "bjk", label: "Billie Jean King Cup", href: "/tournaments?name=bjk" },
];

type RankedRow = { row: PlayerRow; tour: "ATP" | "WTA" };

function sideName(side: ScoreCard["home"]): string {
  return side.fullName?.trim() || side.name;
}

function hasPortrait(match: ScoreCard): boolean {
  return Boolean(match.home.photoUrl || match.away.photoUrl);
}

function compareEditorial(a: ScoreCard, b: ScoreCard): number {
  const circuit = a.circuitRank - b.circuitRank;
  if (circuit !== 0) return circuit;
  const live = (match: ScoreCard) =>
    match.status === "live" ? 0 : match.status === "upcoming" ? 1 : 2;
  const status = live(a) - live(b);
  if (status !== 0) return status;
  return Number(hasPortrait(b)) - Number(hasPortrait(a));
}

function matchMedia(wiki: Map<string, WikiPlayerMedia>, match: ScoreCard): WikiPlayerMedia | undefined {
  const home = wiki.get(sideName(match.home));
  const away = wiki.get(sideName(match.away));
  return home?.imageSrc ? home : away?.imageSrc ? away : home ?? away;
}

function buildTourPulse(items: ScoreCard[], featuredMatch: ScoreCard): TourPulse {
  const circuits = CIRCUITS.flatMap((circuit) => {
    const matches = items.filter((item) => classifyTournamentName(item.tournament) === circuit.id);
    if (matches.length === 0) return [];
    return [
      {
        ...circuit,
        liveCount: matches.filter((item) => item.status === "live").length,
        upcomingCount: matches.filter((item) => item.status === "upcoming").length,
        sample: matches[0]?.tournament ?? null,
      },
    ];
  });
  return {
    featured: {
      href: featuredMatch.href,
      title: `${featuredMatch.home.name} vs ${featuredMatch.away.name}`,
      meta: `${featuredMatch.tournament} · ${featuredMatch.round}`,
      status: featuredMatch.status,
    },
    circuits,
  };
}

function pickRankedPortraits(atp: RankedRow[], wta: RankedRow[], wiki: Map<string, WikiPlayerMedia>): RankedRow[] {
  const withPhoto = (entry: RankedRow) => Boolean(wiki.get(entry.row.name)?.imageSrc);
  const picked = [...atp.filter(withPhoto).slice(0, 2), ...wta.filter(withPhoto).slice(0, 2)];
  if (picked.length >= 4) return picked.slice(0, 4);
  const rest = [...atp, ...wta].filter(
    (entry) => withPhoto(entry) && !picked.some((row) => row.row.id === entry.row.id),
  );
  return [...picked, ...rest].slice(0, 4);
}

function tournamentNameOf(match: { metadata?: Record<string, unknown> }): string {
  const meta = match.metadata ?? {};
  if (typeof meta.tournamentName === "string" && meta.tournamentName) return meta.tournamentName;
  if (typeof meta.tournamentShortName === "string" && meta.tournamentShortName) {
    return meta.tournamentShortName;
  }
  return "";
}

async function resolveHomeReplay(): Promise<HomeReplayFeature | null> {
  const boards = await Promise.all(
    (["IN_PROGRESS", "SUSPENDED", "COMPLETED"] as const).map((status) =>
      fetchUpstreamMatches({ status, page: 0, size: 100 }).catch(() => []),
    ),
  );
  const matches = [...new Map(boards.flat().map((match) => [match.id, match])).values()];
  const ranked = rankHomeReplayCandidates(
    matches.map((match) => ({
      id: match.id,
      status: match.status,
      pointsPlayed: match.pointsPlayed,
      circuitRank: matchCircuitRank(match),
      tournament: tournamentNameOf(match),
      endedAt: match.endedAt,
      scheduledAt: match.scheduledAt,
    })),
    process.env.HOME_FALLBACK_REPLAY_MATCH_ID,
  );
  const configuredFallback = process.env.HOME_FALLBACK_REPLAY_MATCH_ID?.trim();
  const shortlist = ranked.slice(0, 8);
  const fallbackPick = configuredFallback
    ? ranked.find((pick) => pick.id === configuredFallback)
    : undefined;
  if (fallbackPick && !shortlist.some((pick) => pick.id === fallbackPick.id)) {
    shortlist.push(fallbackPick);
  }
  const panels: Array<{
    picked: (typeof shortlist)[number];
    match: (typeof matches)[number];
    panel: ReturnType<typeof toMatchCentrePanel>;
  }> = [];
  for (const picked of shortlist) {
    let match = matches.find((row) => row.id === picked.id) ?? null;
    if (!match) {
      try {
        match = await fetchUpstreamMatch(picked.id);
      } catch {
        match = null;
      }
    }
    if (!match) continue;
    try {
      panels.push({ picked, match, panel: toMatchCentrePanel(match) });
    } catch {
      continue;
    }
  }
  if (panels.length === 0) return null;
  const photos = await headshotByName(
    panels.flatMap(({ panel }) => [panel.home.name, panel.away.name]),
  );
  const featured = panels.find(
    ({ panel }) => photos.get(panel.home.name) && photos.get(panel.away.name),
  );
  if (!featured) return null;
  if (!(await isReplayPointReady(featured.match.id))) return null;
  const { picked, match, panel } = featured;
  return {
    matchId: match.id,
    href: `/matches/${match.externalId?.trim() || match.id}`,
    kind: picked.kind,
    homeName: panel.home.name,
    awayName: panel.away.name,
    homePhotoUrl: photos.get(panel.home.name),
    awayPhotoUrl: photos.get(panel.away.name),
    homePlayerId: panel.home.id,
    awayPlayerId: panel.away.id,
    tournament: panel.tournament,
    round: panel.round,
    surface: panel.surface,
    status: panel.status,
    score: panel.score,
  };
}

export async function getHomeContent(): Promise<HomeContent> {
  const [feed, atpBoard, wtaBoard, replay] = await Promise.all([
    getScoresFeed(),
    getPlayersBoard("atp").catch(() => ({ tour: "atp" as const, updatedAt: "", rows: [] })),
    getPlayersBoard("wta").catch(() => ({ tour: "wta" as const, updatedAt: "", rows: [] })),
    resolveHomeReplay(),
  ]);
  const ordered = [...feed.items].sort(compareEditorial);
  const photoMatches = ordered.filter(hasPortrait).slice(0, 8);
  const moreMatches = ordered.filter((match) => !hasPortrait(match)).slice(0, 10);
  const featuredMatch =
    photoMatches[0] ?? ordered.find((item) => item.status === "live") ?? ordered[0] ?? null;
  const atpRanked = atpBoard.rows.slice(0, 8).map((row) => ({ row, tour: "ATP" as const }));
  const wtaRanked = wtaBoard.rows.slice(0, 8).map((row) => ({ row, tour: "WTA" as const }));
  const wikiNames = [
    ...photoMatches.flatMap((match) => [sideName(match.home), sideName(match.away)]),
    ...(featuredMatch ? [sideName(featuredMatch.home), sideName(featuredMatch.away)] : []),
    ...atpRanked.map((entry) => entry.row.name),
    ...wtaRanked.map((entry) => entry.row.name),
  ];
  const wiki = await fetchWikiPlayerMediaMap(wikiNames);
  const onCourt: HomeStory[] = photoMatches.map((match) => {
    const media = matchMedia(wiki, match);
    const imageSrc = media?.imageSrc ?? match.home.photoUrl ?? match.away.photoUrl ?? null;
    return {
      id: match.id,
      tag: match.status === "live" ? "Live" : "On court",
      title: `${match.home.name} vs ${match.away.name} · ${match.tournament}`,
      href: matchStoryHref(match.href),
      imageSrc,
      imageAlt: media?.imageAlt || `${match.home.name} versus ${match.away.name}`,
      imageCredit: media?.credit ?? (imageSrc ? "Photo: Wikimedia Commons" : null),
      summary: media?.extract ?? null,
      publishedLabel: match.status === "live" ? "Live now" : match.round,
    };
  });
  const onCourtMore: HomeMoreItem[] = moreMatches.map((match) => ({
    id: match.id,
    href: match.href,
    eyebrow: match.tournament,
    title: `${match.home.name} vs ${match.away.name}`,
    meta: match.status === "live" ? "Live" : match.round,
  }));
  const playerProfiles: HomeStory[] = pickRankedPortraits(atpRanked, wtaRanked, wiki).map(
    ({ row, tour }) => {
      const media = wiki.get(row.name);
      return {
        id: `${tour.toLowerCase()}-${row.id}`,
        tag: "Player profile",
        title: `${tour} #${row.rank} ${row.name} · ${row.points} pts`,
        href: playerStoryHref(row.id),
        imageSrc: media?.imageSrc ?? null,
        imageAlt: media?.imageAlt || row.name,
        imageCredit: media?.credit ?? null,
        summary: media?.extract ?? null,
        publishedLabel: `${tour} rankings`,
      };
    },
  );
  const empty = feed.items.length === 0 && playerProfiles.length === 0;
  const matchHref = featuredMatch?.href ?? "/matches";
  const matchTitle = featuredMatch
    ? `${featuredMatch.home.name} vs ${featuredMatch.away.name}`
    : "Live Centre";
  // Wiki infoboxes are headshots — they look wrong as a full-bleed cinema plate.
  const featuredPhoto = IMG.grassAction;
  const featuredAlt = "Tennis player celebrating on a grass court";
  return {
    hero: {
      headline: empty ? "Waiting on live tennis data" : "Replay every point from centre court",
      ctaLabel: "Open Live Centre",
      ctaHref: "/matches",
      imageSrc: featuredPhoto,
      imageAlt: featuredAlt,
    },
    tourPulse: featuredMatch ? buildTourPulse(feed.items, featuredMatch) : null,
    onCourt,
    onCourtMore,
    featured: featuredMatch
      ? {
          eyebrow: "Match Centre",
          headline: matchTitle,
          label: featuredMatch.status === "live" ? "LIVE" : "Featured",
          href: matchHref,
          imageSrc: featuredPhoto,
          imageAlt: featuredAlt,
        }
      : null,
    playerProfiles,
    empty,
    replay,
  };
}
