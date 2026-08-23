import { fetchWikiPlayerMediaMap, readMinutesFromExtract } from "@/lib/wikipedia-upstream";
import { getPlayersBoard } from "@/services/catalogue";
import { getScoresFeed } from "@/services/scores";

export type StoryTag = "News" | "Feature" | "Analysis" | "Live" | "Profile";

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
  readMinutes: number | null;
};

export type HomeContent = {
  hero: {
    headline: string;
    ctaLabel: string;
    ctaHref: string;
    imageSrc: string;
    imageAlt: string;
  };
  editorsPicks: HomeStory[];
  featured: {
    eyebrow: string;
    headline: string;
    label: string;
    href: string;
    imageSrc: string;
    imageAlt: string;
  } | null;
  latest: HomeStory[];
  empty: boolean;
};

const IMG = {
  grassAction:
    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=2400&q=80",
} as const;

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

  const editorsPicks: HomeStory[] = pickSource.map((match) => {
    const home = wiki.get(match.home.name);
    const away = wiki.get(match.away.name);
    const media = home?.imageSrc ? home : away?.imageSrc ? away : home ?? away;
    const summary = media?.extract ?? null;
    return {
      id: match.id,
      tag: match.status === "live" ? "Live" : "News",
      title: `${match.home.name} vs ${match.away.name} · ${match.tournament}`,
      href: match.href,
      imageSrc: media?.imageSrc ?? null,
      imageAlt: media?.imageAlt || `${match.home.name} versus ${match.away.name}`,
      imageCredit: media?.credit ?? null,
      summary,
      publishedLabel: match.status === "live" ? "Live now" : match.round,
      readMinutes: readMinutesFromExtract(summary),
    };
  });

  const latest: HomeStory[] = rankingRows.map(({ row, tour }) => {
    const media = wiki.get(row.name);
    const summary = media?.extract ?? null;
    return {
      id: `${tour.toLowerCase()}-${row.id}`,
      tag: summary ? "Profile" : "Analysis",
      title: `${tour} #${row.rank} ${row.name} · ${row.points} pts`,
      href: row.href,
      imageSrc: media?.imageSrc ?? null,
      imageAlt: media?.imageAlt || row.name,
      imageCredit: media?.credit ?? null,
      summary,
      publishedLabel: `${tour} rankings`,
      readMinutes: readMinutesFromExtract(summary),
    };
  });

  const empty = editorsPicks.length === 0 && latest.length === 0;
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
    editorsPicks,
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
    latest,
    empty,
  };
}
