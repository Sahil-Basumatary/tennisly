import { getPlayersBoard } from "@/services/catalogue";
import { getScoresFeed } from "@/services/scores";

export type StoryTag = "News" | "Feature" | "Analysis" | "Live";

export type HomeStory = {
  id: string;
  tag: StoryTag;
  title: string;
  href: string;
  imageSrc: string;
  imageAlt: string;
  publishedLabel: string;
  readMinutes: number;
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
  serve:
    "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?auto=format&fit=crop&w=1200&q=80",
  crowd:
    "https://images.unsplash.com/photo-1599586120429-48281b6f0ece?auto=format&fit=crop&w=1200&q=80",
} as const;

export async function getHomeContent(): Promise<HomeContent> {
  const [feed, atpBoard, wtaBoard] = await Promise.all([
    getScoresFeed(),
    getPlayersBoard("atp").catch(() => ({ tour: "atp" as const, updatedAt: "", rows: [] })),
    getPlayersBoard("wta").catch(() => ({ tour: "wta" as const, updatedAt: "", rows: [] })),
  ]);

  const live = feed.items.filter((item) => item.status === "live");
  const featuredMatch = live[0] ?? feed.items[0] ?? null;
  const editorsPicks: HomeStory[] = [];

  for (const match of [...live, ...feed.items.filter((item) => item.status !== "live")].slice(
    0,
    5,
  )) {
    editorsPicks.push({
      id: match.id,
      tag: match.status === "live" ? "Live" : "News",
      title: `${match.home.name} vs ${match.away.name} · ${match.tournament}`,
      href: match.href,
      imageSrc: IMG.serve,
      imageAlt: `${match.home.name} versus ${match.away.name}`,
      publishedLabel: match.status === "live" ? "Live now" : match.round,
      readMinutes: 3,
    });
  }

  const rankingStories: HomeStory[] = [];
  for (const row of atpBoard.rows.slice(0, 2)) {
    rankingStories.push({
      id: `atp-${row.id}`,
      tag: "Analysis",
      title: `ATP #${row.rank} ${row.name} · ${row.points} pts`,
      href: row.href,
      imageSrc: IMG.crowd,
      imageAlt: row.name,
      publishedLabel: "ATP rankings",
      readMinutes: 2,
    });
  }
  for (const row of wtaBoard.rows.slice(0, 2)) {
    rankingStories.push({
      id: `wta-${row.id}`,
      tag: "Analysis",
      title: `WTA #${row.rank} ${row.name} · ${row.points} pts`,
      href: row.href,
      imageSrc: IMG.crowd,
      imageAlt: row.name,
      publishedLabel: "WTA rankings",
      readMinutes: 2,
    });
  }

  const empty = editorsPicks.length === 0 && rankingStories.length === 0;
  const matchHref = featuredMatch?.href ?? "/matches";
  const matchTitle = featuredMatch
    ? `${featuredMatch.home.name} vs ${featuredMatch.away.name}`
    : "Live Centre";

  return {
    hero: {
      headline: empty
        ? "Waiting on live tennis data"
        : "Replay every point from centre court",
      ctaLabel: "Open Live Centre",
      ctaHref: "/matches",
      imageSrc: IMG.grassAction,
      imageAlt: "Tennis player celebrating on a grass court",
    },
    editorsPicks,
    featured: featuredMatch
      ? {
          eyebrow: "Match Centre",
          headline: matchTitle,
          label: featuredMatch.status === "live" ? "LIVE" : "Featured",
          href: matchHref,
          imageSrc: IMG.grassAction,
          imageAlt: "Stadium lights over a tennis arena",
        }
      : null,
    latest: rankingStories,
    empty,
  };
}
