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

const ALLOWED_TYPES = new Set(["image/jpeg", "image/jpg", "image/png", "image/webp"]);

function sniffImageType(bytes: Uint8Array): string | null {
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return "image/jpeg";
  }
  if (
    bytes.length >= 8 &&
    bytes[0] === 0x89 &&
    bytes[1] === 0x50 &&
    bytes[2] === 0x4e &&
    bytes[3] === 0x47
  ) {
    return "image/png";
  }
  if (
    bytes.length >= 12 &&
    bytes[0] === 0x52 &&
    bytes[1] === 0x49 &&
    bytes[2] === 0x46 &&
    bytes[3] === 0x46 &&
    bytes[8] === 0x57 &&
    bytes[9] === 0x45 &&
    bytes[10] === 0x42 &&
    bytes[11] === 0x50
  ) {
    return "image/webp";
  }
  return null;
}

export function commonsImageContentType(headerType: string, bytes: Uint8Array): string | null {
  const header = headerType.split(";")[0].trim().toLowerCase();
  if (ALLOWED_TYPES.has(header)) return header === "image/jpg" ? "image/jpeg" : header;
  return sniffImageType(bytes);
}

/** Follow Commons redirects only onto upload.wikimedia.org — never an open redirect. */
export async function fetchAllowlistedCommons(target: URL): Promise<Response | null> {
  let current = target;
  for (let hop = 0; hop < 3; hop++) {
    let response: Response;
    try {
      response = await fetch(current, {
        headers: {
          Accept: "image/jpeg,image/png,image/webp,image/*;q=0.8,*/*;q=0.5",
          "User-Agent": WIKIMEDIA_USER_AGENT,
          "Api-User-Agent": WIKIMEDIA_USER_AGENT,
        },
        redirect: "manual",
        signal: AbortSignal.timeout(8000),
      });
    } catch {
      return null;
    }
    if (response.status >= 300 && response.status < 400) {
      const location = response.headers.get("location");
      if (!location) return null;
      const next = parseCommonsPhotoUrl(new URL(location, current).toString());
      if (!next) return null;
      current = next;
      continue;
    }
    return response;
  }
  return null;
}
