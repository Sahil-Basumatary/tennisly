export type WikiPlayerMedia = {
  imageSrc: string | null;
  imageAlt: string;
  extract: string | null;
  credit: string | null;
};

const WIKI_UA = "Tennisly/1.0 (https://tennisly.tv; hello@tennisly.dev)";
const EMPTY: WikiPlayerMedia = {
  imageSrc: null,
  imageAlt: "",
  extract: null,
  credit: null,
};

type WikiSummary = {
  type?: string;
  title?: string;
  extract?: string;
  thumbnail?: { source?: string };
};

type WikiSearch = {
  query?: { search?: { title?: string }[] };
};

function wikiHeaders(): HeadersInit {
  return {
    Accept: "application/json",
    "User-Agent": WIKI_UA,
    "Api-User-Agent": WIKI_UA,
  };
}

function clipExtract(text: string): string {
  const compact = text.replace(/\s+/g, " ").trim();
  if (compact.length <= 220) return compact;
  const cut = compact.slice(0, 220);
  const lastSpace = cut.lastIndexOf(" ");
  return `${(lastSpace > 80 ? cut.slice(0, lastSpace) : cut).trim()}…`;
}

function isCommonsPhoto(url: string): boolean {
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== "https:") return false;
    if (parsed.hostname !== "upload.wikimedia.org") return false;
    return /\/wikipedia\/.+\.(jpe?g|png|webp)(\/|$)/i.test(parsed.pathname);
  } catch {
    return false;
  }
}

function searchQueries(name: string): string[] {
  const trimmed = name.trim();
  if (!trimmed) return [];
  const queries = [`${trimmed} tennis`, trimmed];
  const last = trimmed.split(/\s+/).at(-1);
  if (last && last.length > 2 && last !== trimmed) queries.push(`${last} tennis`);
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

async function fetchSummary(title: string): Promise<WikiSummary | null> {
  const path = encodeURIComponent(title.replaceAll(" ", "_"));
  const summary = await wikiGet<WikiSummary>(
    `https://en.wikipedia.org/api/rest_v1/page/summary/${path}`,
  );
  if (!summary || summary.type === "disambiguation") return null;
  return summary;
}

async function searchTitle(query: string): Promise<string | null> {
  const params = new URLSearchParams({
    action: "query",
    list: "search",
    srsearch: query,
    srlimit: "1",
    format: "json",
  });
  const data = await wikiGet<WikiSearch>(`https://en.wikipedia.org/w/api.php?${params}`);
  const title = data?.query?.search?.[0]?.title?.trim();
  return title || null;
}

function mediaFromSummary(name: string, summary: WikiSummary): WikiPlayerMedia {
  const thumb = summary.thumbnail?.source?.trim() ?? "";
  const extract = summary.extract ? clipExtract(summary.extract) : null;
  const okPhoto = thumb.length > 0 && isCommonsPhoto(thumb);
  return {
    imageSrc: okPhoto ? thumb : null,
    imageAlt: okPhoto ? `${summary.title ?? name}` : name,
    extract,
    credit: okPhoto ? "Photo: Wikimedia Commons" : null,
  };
}

export async function fetchWikiPlayerMedia(name: string): Promise<WikiPlayerMedia> {
  const trimmed = name.trim();
  if (!trimmed) return EMPTY;
  const direct = await fetchSummary(trimmed);
  if (direct?.extract || direct?.thumbnail?.source) return mediaFromSummary(trimmed, direct);
  for (const query of searchQueries(trimmed)) {
    const title = await searchTitle(query);
    if (!title) continue;
    const summary = await fetchSummary(title);
    if (summary?.extract || summary?.thumbnail?.source) {
      return mediaFromSummary(trimmed, summary);
    }
  }
  return { ...EMPTY, imageAlt: trimmed };
}

export async function fetchWikiPlayerMediaMap(
  names: string[],
): Promise<Map<string, WikiPlayerMedia>> {
  const unique = [...new Set(names.map((name) => name.trim()).filter(Boolean))];
  const entries = await Promise.all(
    unique.map(async (name) => [name, await fetchWikiPlayerMedia(name)] as const),
  );
  return new Map(entries);
}

export function playerInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[parts.length - 1][0] ?? ""}`.toUpperCase();
}

export function readMinutesFromExtract(extract: string | null): number | null {
  if (!extract) return null;
  const words = extract.split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.round(words / 200));
}
