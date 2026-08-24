import { parseCommonsPhotoUrl, proxiedCommonsSrc, WIKIMEDIA_USER_AGENT } from "@/lib/commons-photo";

export type WikiPlayerMedia = {
  imageSrc: string | null;
  imageAlt: string;
  extract: string | null;
  extractFull: string | null;
  credit: string | null;
  sourceTitle: string | null;
  sourceUrl: string | null;
};
const EMPTY: WikiPlayerMedia = {
  imageSrc: null,
  imageAlt: "",
  extract: null,
  extractFull: null,
  credit: null,
  sourceTitle: null,
  sourceUrl: null,
};

type WikiSummary = {
  type?: string;
  title?: string;
  description?: string;
  extract?: string;
  thumbnail?: { source?: string };
  content_urls?: { desktop?: { page?: string } };
};

type WikiSearch = {
  query?: { search?: { title?: string }[] };
};

function wikiHeaders(): HeadersInit {
  return {
    Accept: "application/json",
    "User-Agent": WIKIMEDIA_USER_AGENT,
    "Api-User-Agent": WIKIMEDIA_USER_AGENT,
  };
}

function clipExtract(text: string, max = 220): string {
  const compact = text.replace(/\s+/g, " ").trim();
  if (compact.length <= max) return compact;
  const cut = compact.slice(0, max);
  const lastSpace = cut.lastIndexOf(" ");
  return `${(lastSpace > 80 ? cut.slice(0, lastSpace) : cut).trim()}…`;
}

function isCommonsPhoto(url: string): boolean {
  return parseCommonsPhotoUrl(url) !== null;
}

function fold(value: string): string {
  return value
    .normalize("NFKD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

function isAbbreviatedName(name: string): boolean {
  return /^[A-Za-z]\.\s/.test(name.trim()) || /\s[A-Za-z]\.\s/.test(name.trim());
}

function looksLikeTennis(summary: WikiSummary): boolean {
  const blob = `${summary.description ?? ""} ${summary.extract ?? ""}`.toLowerCase();
  return /\btennis\b|grand slam|\batp\b|\bwta\b|davis cup|wimbledon|roland garros/.test(blob);
}

function namesAlign(query: string, title: string): boolean {
  const queryTokens = fold(query).split(" ").filter(Boolean);
  const titleTokens = fold(title).split(" ").filter(Boolean);
  const queryLast = queryTokens.at(-1) ?? "";
  if (!queryLast || queryLast.length < 3 || !titleTokens.includes(queryLast)) return false;
  if (queryTokens.length < 2) return true;
  return (queryTokens[0] ?? "").charAt(0) === (titleTokens[0] ?? "").charAt(0);
}

function searchQueries(name: string): string[] {
  const trimmed = name.trim();
  if (!trimmed) return [];
  const queries = [`${trimmed} tennis`, trimmed];
  if (!isAbbreviatedName(trimmed)) {
    const last = trimmed.split(/\s+/).at(-1);
    if (last && last.length > 3 && last !== trimmed) queries.push(`${last} tennis`);
  }
  return queries;
}

async function wikiGet<T>(url: string): Promise<T | null> {
  try {
    const response = await fetch(url, {
      headers: wikiHeaders(),
      signal: AbortSignal.timeout(2500),
      next: { revalidate: 86_400 },
    });
    if (!response.ok) return null;
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

async function fetchSummary(lookupTitle: string, originalName: string): Promise<WikiSummary | null> {
  const path = encodeURIComponent(lookupTitle.replaceAll(" ", "_"));
  const summary = await wikiGet<WikiSummary>(
    `https://en.wikipedia.org/api/rest_v1/page/summary/${path}`,
  );
  if (!summary || summary.type === "disambiguation") return null;
  if (!looksLikeTennis(summary) || !namesAlign(originalName, summary.title ?? lookupTitle)) {
    return null;
  }
  return summary;
}

async function searchTitle(query: string, originalName: string): Promise<string | null> {
  const params = new URLSearchParams({
    action: "query",
    list: "search",
    srsearch: query,
    srlimit: "1",
    format: "json",
  });
  const data = await wikiGet<WikiSearch>(`https://en.wikipedia.org/w/api.php?${params}`);
  const title = data?.query?.search?.[0]?.title?.trim();
  if (!title || !namesAlign(originalName, title)) return null;
  return title;
}

function sizedPortrait(url: string): string {
  // Commons rejects nonstandard hotlink widths; 330px is the smallest suitable profile-card step.
  return url.replace(/\/\d+px-/, "/330px-");
}

function mediaFromSummary(name: string, summary: WikiSummary): WikiPlayerMedia {
  const thumb = summary.thumbnail?.source?.trim() ?? "";
  const full = summary.extract?.replace(/\s+/g, " ").trim() || null;
  const okPhoto = thumb.length > 0 && isCommonsPhoto(thumb);
  const sourceUrl = summary.content_urls?.desktop?.page?.trim() || null;
  return {
    imageSrc: okPhoto ? proxiedCommonsSrc(sizedPortrait(thumb)) : null,
    imageAlt: okPhoto ? `${summary.title ?? name}` : name,
    extract: full ? clipExtract(full) : null,
    extractFull: full ? clipExtract(full, 1200) : null,
    credit: okPhoto ? "Photo: Wikimedia Commons" : null,
    sourceTitle: summary.title ?? null,
    sourceUrl,
  };
}

const MEDIA_TTL_MS = 24 * 60 * 60 * 1000;
const mediaMemo = new Map<string, { media: WikiPlayerMedia; exp: number }>();

export async function fetchWikiPlayerMedia(name: string): Promise<WikiPlayerMedia> {
  const trimmed = name.trim();
  if (!trimmed) return EMPTY;
  const cached = mediaMemo.get(trimmed);
  if (cached && cached.exp > Date.now()) return cached.media;
  let media: WikiPlayerMedia = { ...EMPTY, imageAlt: trimmed };
  if (!isAbbreviatedName(trimmed)) {
    const direct = await fetchSummary(trimmed, trimmed);
    if (direct) media = mediaFromSummary(trimmed, direct);
  }
  if (!media.imageSrc && !media.extract) {
    for (const query of searchQueries(trimmed)) {
      const title = await searchTitle(query, trimmed);
      if (!title) continue;
      const summary = await fetchSummary(title, trimmed);
      if (summary) {
        media = mediaFromSummary(trimmed, summary);
        break;
      }
    }
  }
  mediaMemo.set(trimmed, {
    media,
    exp: Date.now() + (media.imageSrc ? MEDIA_TTL_MS : 15 * 60 * 1000),
  });
  return media;
}

export async function fetchWikiPlayerMediaMap(
  names: string[],
): Promise<Map<string, WikiPlayerMedia>> {
  const unique = [...new Set(names.map((name) => name.trim()).filter(Boolean))];
  const map = new Map<string, WikiPlayerMedia>();
  const chunkSize = 8;
  for (let i = 0; i < unique.length; i += chunkSize) {
    const chunk = unique.slice(i, i + chunkSize);
    const entries = await Promise.all(
      chunk.map(async (name) => [name, await fetchWikiPlayerMedia(name)] as const),
    );
    for (const [name, media] of entries) map.set(name, media);
  }
  return map;
}

export function playerInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[parts.length - 1][0] ?? ""}`.toUpperCase();
}
