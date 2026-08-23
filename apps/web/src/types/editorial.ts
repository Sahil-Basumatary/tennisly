export type EditorialKind = "match" | "player";

export type EditorialFact = {
  label: string;
  value: string;
};

export type EditorialLink = {
  label: string;
  href: string;
};

export type EditorialStory = {
  slug: string;
  kind: EditorialKind;
  label: "On court" | "Player profile";
  headline: string;
  dek: string;
  imageSrc: string | null;
  imageAlt: string;
  imageCredit: string | null;
  extract: string | null;
  sourceTitle: string | null;
  sourceUrl: string | null;
  facts: EditorialFact[];
  related: EditorialLink[];
  primaryCta: EditorialLink;
  secondaryCta?: EditorialLink;
};

export type StoryResult =
  | { status: "ok"; story: EditorialStory }
  | { status: "missing" }
  | { status: "unavailable" };

export function parseStorySlug(
  slug: string,
): { kind: EditorialKind; id: string } | null {
  if (slug.startsWith("match-") && slug.length > "match-".length) {
    return { kind: "match", id: slug.slice("match-".length) };
  }
  if (slug.startsWith("player-") && slug.length > "player-".length) {
    return { kind: "player", id: slug.slice("player-".length) };
  }
  return null;
}

export function matchStoryHref(matchHref: string): string {
  const id = matchHref.startsWith("/matches/")
    ? matchHref.slice("/matches/".length)
    : matchHref.replace(/^\/+/, "");
  return `/stories/match-${id}`;
}

export function playerStoryHref(playerId: string): string {
  return `/stories/player-${playerId}`;
}
