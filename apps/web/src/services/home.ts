export type StoryTag = "News" | "Feature" | "Analysis" | "Video";

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
  };
  latest: HomeStory[];
};

const IMG = {
  grassAction:
    "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=2400&q=80",
  serve:
    "https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?auto=format&fit=crop&w=1200&q=80",
  racket:
    "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=1200&q=80",
  clayBall:
    "https://images.unsplash.com/photo-1534158914592-062992fbe900?auto=format&fit=crop&w=1200&q=80",
  crowd:
    "https://images.unsplash.com/photo-1599586120429-48281b6f0ece?auto=format&fit=crop&w=1200&q=80",
  gear:
    "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?auto=format&fit=crop&w=1200&q=80",
} as const;

export async function getHomeContent(): Promise<HomeContent> {
  return {
    hero: {
      headline: "Replay every point from centre court",
      ctaLabel: "Learn More",
      ctaHref: "/matches",
      imageSrc: IMG.grassAction,
      imageAlt: "Tennis player celebrating on a grass court",
    },
    editorsPicks: [
      {
        id: "pick-1",
        tag: "News",
        title: "How Alcaraz turned a break point into the set",
        href: "/matches/m-alcaraz-sinner",
        imageSrc: IMG.serve,
        imageAlt: "Player serving on outdoor court",
        publishedLabel: "Yesterday",
        readMinutes: 6,
      },
      {
        id: "pick-2",
        tag: "Feature",
        title: "Inside the physics behind a kicking second serve",
        href: "/matches?view=replays",
        imageSrc: IMG.racket,
        imageAlt: "Close-up of tennis racket and ball",
        publishedLabel: "12 Jul 2026",
        readMinutes: 4,
      },
      {
        id: "pick-3",
        tag: "Analysis",
        title: "Why baseline depth decided the third-set tiebreak",
        href: "/scores",
        imageSrc: IMG.clayBall,
        imageAlt: "Tennis ball bouncing on clay",
        publishedLabel: "2 days ago",
        readMinutes: 5,
      },
      {
        id: "pick-4",
        tag: "News",
        title: "Swiatek’s return patterns mapped shot by shot",
        href: "/players",
        imageSrc: IMG.crowd,
        imageAlt: "Crowd watching a tennis match",
        publishedLabel: "3 days ago",
        readMinutes: 4,
      },
      {
        id: "pick-5",
        tag: "Feature",
        title: "Court visualisation: reading spin from trajectory alone",
        href: "/matches",
        imageSrc: IMG.gear,
        imageAlt: "Tennis balls and racket on court",
        publishedLabel: "4 days ago",
        readMinutes: 7,
      },
    ],
    featured: {
      eyebrow: "Tennisly Originals",
      headline: "Member exclusives",
      label: "Featured",
      href: "/sign-up",
      imageSrc: IMG.grassAction,
      imageAlt: "Stadium lights over a tennis arena",
    },
    latest: [
      {
        id: "latest-1",
        tag: "News",
        title: "Live centre now streams point markers in under 200ms",
        href: "/matches?status=live",
        imageSrc: IMG.crowd,
        imageAlt: "Crowd watching a tennis match",
        publishedLabel: "Today",
        readMinutes: 3,
      },
      {
        id: "latest-2",
        tag: "Analysis",
        title: "Surface bounce models: grass vs hard court coefficients",
        href: "/tournaments",
        imageSrc: IMG.grassAction,
        imageAlt: "Grass court tennis action",
        publishedLabel: "Yesterday",
        readMinutes: 8,
      },
      {
        id: "latest-3",
        tag: "Feature",
        title: "Building trust into every replay frame checksum",
        href: "/dashboard",
        imageSrc: IMG.gear,
        imageAlt: "Tennis balls and racket on court",
        publishedLabel: "5 days ago",
        readMinutes: 5,
      },
      {
        id: "latest-4",
        tag: "Video",
        title: "Watch: full-rally reconstruction at 60 frames per second",
        href: "/matches?view=replays",
        imageSrc: IMG.serve,
        imageAlt: "Player hitting a serve",
        publishedLabel: "1 week ago",
        readMinutes: 2,
      },
    ],
  };
}
