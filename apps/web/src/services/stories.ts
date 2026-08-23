import { fetchWikiPlayerMedia, fetchWikiPlayerMediaMap } from "@/lib/wikipedia-upstream";
import { surfaceLabel } from "@/lib/tournament-filter";
import { getMatchCentre, getPlayerProfile } from "@/services/catalogue";
import type { EditorialStory, StoryResult } from "@/types/editorial";
import { parseStorySlug, playerStoryHref } from "@/types/editorial";
import type { MatchCentrePanel } from "@/types/scaffolds";

function scoreLine(match: MatchCentrePanel): string {
  const homeSets = [...match.score.homeSets];
  const awaySets = [...match.score.awaySets];
  if (match.status === "live") {
    homeSets.push(match.score.homeGames);
    awaySets.push(match.score.awayGames);
  }
  const home = homeSets.length > 0 ? homeSets.join(" ") : "—";
  const away = awaySets.length > 0 ? awaySets.join(" ") : "—";
  return `${home}  ${away}`;
}

function statusLabel(status: MatchCentrePanel["status"]): string {
  if (status === "live") return "Live";
  if (status === "final") return "Final";
  return "Upcoming";
}

async function matchStory(id: string, slug: string): Promise<StoryResult> {
  const match = await getMatchCentre(id);
  if (!match) return { status: "missing" };
  const wiki = await fetchWikiPlayerMediaMap([match.home.name, match.away.name]);
  const homeMedia = wiki.get(match.home.name);
  const awayMedia = wiki.get(match.away.name);
  const media = homeMedia?.imageSrc ? homeMedia : awayMedia?.imageSrc ? awayMedia : homeMedia ?? awayMedia;
  const extract = media?.extractFull ?? media?.extract ?? null;
  const story: EditorialStory = {
    slug,
    kind: "match",
    label: "On court",
    headline: `${match.home.name} vs ${match.away.name}`,
    dek: `${match.tournament} · ${match.round}`,
    imageSrc: media?.imageSrc ?? null,
    imageAlt: media?.imageAlt || `${match.home.name} versus ${match.away.name}`,
    imageCredit: media?.credit ?? null,
    extract,
    sourceTitle: media?.sourceTitle ?? null,
    sourceUrl: media?.sourceUrl ?? null,
    facts: [
      { label: "Tournament", value: match.tournament },
      { label: "Round", value: match.round },
      { label: "Status", value: statusLabel(match.status) },
      { label: "Score", value: scoreLine(match) },
      { label: "Surface", value: surfaceLabel(match.surface) },
      { label: "Court", value: match.court },
    ],
    related: [
      { label: match.home.name, href: playerStoryHref(match.home.id) },
      { label: match.away.name, href: playerStoryHref(match.away.id) },
      { label: "Match Centre", href: `/matches/${id}` },
    ],
    primaryCta: { label: "Open Match Centre", href: `/matches/${id}` },
    secondaryCta: {
      label: "Open Player Analytics",
      href: `/analytics/players/${match.home.id}`,
    },
  };
  return { status: "ok", story };
}

async function playerStory(id: string, slug: string): Promise<StoryResult> {
  const result = await getPlayerProfile(id);
  if (result.status === "missing") return { status: "missing" };
  if (result.status === "unavailable") return { status: "unavailable" };
  const player = result.player;
  const media = await fetchWikiPlayerMedia(player.name);
  const tourLabel = player.tour === "wta" ? "WTA" : "ATP";
  const rankLabel = player.rank != null ? `${tourLabel} #${player.rank}` : `${tourLabel} rankings`;
  const story: EditorialStory = {
    slug,
    kind: "player",
    label: "Player profile",
    headline: player.name,
    dek: `${player.country} · ${rankLabel}`,
    imageSrc: media.imageSrc,
    imageAlt: media.imageAlt || player.name,
    imageCredit: media.credit,
    extract: media.extractFull ?? media.extract,
    sourceTitle: media.sourceTitle,
    sourceUrl: media.sourceUrl,
    facts: [
      { label: "Tour", value: tourLabel },
      { label: "Rank", value: player.rank != null ? String(player.rank) : "—" },
      { label: "Points", value: player.points != null ? player.points.toLocaleString() : "—" },
      { label: "Country", value: player.country },
    ],
    related: [
      { label: `${tourLabel} rankings`, href: `/players?view=rankings&tour=${player.tour}` },
      { label: "Player board", href: `/players/${player.id}` },
      { label: "Player analytics", href: `/analytics/players/${player.id}` },
    ],
    primaryCta: { label: "Open Player Analytics", href: `/analytics/players/${player.id}` },
    secondaryCta: { label: "Open player board", href: `/players/${player.id}` },
  };
  return { status: "ok", story };
}

export async function getStory(slug: string): Promise<StoryResult> {
  const parsed = parseStorySlug(slug);
  if (!parsed) return { status: "missing" };
  if (parsed.kind === "match") return matchStory(parsed.id, slug);
  return playerStory(parsed.id, slug);
}
