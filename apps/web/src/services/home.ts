import { classifyTournamentName, type CircuitId } from "@/lib/tournament-filter";
import { fetchWikiPlayerMediaMap } from "@/lib/wikipedia-upstream";
import { getPlayersBoard } from "@/services/catalogue";
import { getScoresFeed } from "@/services/scores";
import { matchStoryHref, playerStoryHref } from "@/types/editorial";
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

function buildTourPulse(items: ScoreCard[]): TourPulse | null {
  if (items.length === 0) return null;
  const featuredMatch = items.find((item) => item.status === "live") ?? items[0];
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

export async function getHomeContent(): Promise<HomeContent> {
  const [feed, atpBoard, wtaBoard] = await Promise.all([
    getScoresFeed(),
    getPlayersBoard("atp").catch(() => ({ tour: "atp" as const, updatedAt: "", rows: [] })),
    getPlayersBoard("wta").catch(() => ({ tour: "wta" as const, updatedAt: "", rows: [] })),
  ]);

  const live = feed.items.filter((item) => item.status === "live");
  const featuredMatch = live[0] ?? feed.items[0] ?? null;
  const pickSource = [...live, ...feed.items.filter((item) => item.status !== "live")].slice(0, 5);
  const rankingRows = [
    ...atpBoard.rows.slice(0, 2).map((row) => ({ row, tour: "ATP" as const })),
    ...wtaBoard.rows.slice(0, 2).map((row) => ({ row, tour: "WTA" as const })),
  ];
  const wikiNames = [
    ...pickSource.flatMap((match) => [match.home.name, match.away.name]),
    ...rankingRows.map((entry) => entry.row.name),
  ];
  const wiki = await fetchWikiPlayerMediaMap(wikiNames);

  const onCourt: HomeStory[] = pickSource.map((match) => {
    const home = wiki.get(match.home.name);
    const away = wiki.get(match.away.name);
    const media = home?.imageSrc ? home : away?.imageSrc ? away : home ?? away;
    return {
      id: match.id,
      tag: match.status === "live" ? "Live" : "On court",
      title: `${match.home.name} vs ${match.away.name} · ${match.tournament}`,
      href: matchStoryHref(match.href),
      imageSrc: media?.imageSrc ?? null,
      imageAlt: media?.imageAlt || `${match.home.name} versus ${match.away.name}`,
      imageCredit: media?.credit ?? null,
      summary: media?.extract ?? null,
      publishedLabel: match.status === "live" ? "Live now" : match.round,
    };
  });

  const playerProfiles: HomeStory[] = rankingRows.map(({ row, tour }) => {
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
  });

  const empty = onCourt.length === 0 && playerProfiles.length === 0;
  const matchHref = featuredMatch?.href ?? "/matches";
  const matchTitle = featuredMatch
    ? `${featuredMatch.home.name} vs ${featuredMatch.away.name}`
    : "Live Centre";
  const featuredPhoto =
    (featuredMatch && wiki.get(featuredMatch.home.name)?.imageSrc) ||
    (featuredMatch && wiki.get(featuredMatch.away.name)?.imageSrc) ||
    IMG.grassAction;
  const featuredAlt =
    wiki.get(featuredMatch?.home.name ?? "")?.imageAlt ||
    wiki.get(featuredMatch?.away.name ?? "")?.imageAlt ||
    "Tennis player celebrating on a grass court";

  return {
    hero: {
      headline: empty
        ? "Waiting on live tennis data"
        : "Replay every point from centre court",
      ctaLabel: "Open Live Centre",
      ctaHref: "/matches",
      imageSrc: featuredPhoto,
      imageAlt: featuredAlt,
    },
    tourPulse: buildTourPulse(feed.items),
    onCourt,
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
  };
}
