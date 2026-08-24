export const WIKIMEDIA_USER_AGENT =
  "Tennisly/1.0 (https://tennisly.tv; hello@tennisly.dev)";

const COMMONS_HOST = "upload.wikimedia.org";
const COMMONS_PATH = /^\/wikipedia\/.+\.(jpe?g|png|webp)$/i;

/** Only Wikimedia Commons bitmap thumbs — anything else is SSRF or a useless SVG. */
export function parseCommonsPhotoUrl(raw: string): URL | null {
  try {
    const url = new URL(raw);
    if (url.protocol !== "https:") return null;
    if (url.hostname !== COMMONS_HOST) return null;
    if (url.port !== "") return null;
    if (url.username !== "" || url.password !== "") return null;
    if (url.pathname.includes("..") || url.pathname.toLowerCase().includes("%2e%2e")) return null;
    if (!COMMONS_PATH.test(url.pathname)) return null;
    return url;
  } catch {
    return null;
  }
}

export function proxiedCommonsSrc(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const url = parseCommonsPhotoUrl(raw);
  if (!url) return null;
  return `/api/media/commons?u=${encodeURIComponent(url.toString())}`;
}
