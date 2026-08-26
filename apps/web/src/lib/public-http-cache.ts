import { createHash } from "node:crypto";
import { NextResponse } from "next/server";

export const PUBLIC_CACHE_VARY = "Accept-Encoding";
export const TICKER_CACHE_CONTROL = "public, s-maxage=3, stale-while-revalidate=10";
export const LIVE_CACHE_CONTROL = "public, s-maxage=2, stale-while-revalidate=3";
export const COMPLETED_LIVE_CACHE_CONTROL =
  "public, max-age=86400, s-maxage=86400, immutable";
export const PRIVATE_NO_STORE = "private, no-store, no-cache, must-revalidate";
export const SEALED_REPLAY_CACHE_CONTROL =
  "public, max-age=31536000, s-maxage=31536000, immutable";
export const TRAILING_REPLAY_CACHE_CONTROL =
  "public, s-maxage=2, stale-while-revalidate=3";
export const TICKER_MAX_BYTES = 64_000;
export const LIVE_MAX_BYTES = 16_000;

export function strongEtag(body: string): string {
  const digest = createHash("sha256").update(body).digest("hex").slice(0, 16);
  return `"${digest}"`;
}

export function sequenceEtag(prefix: string, sequence: number): string {
  return `"${prefix}-${Math.max(0, sequence)}"`;
}

export function ifNoneMatch(request: Request, etag: string): boolean {
  const header = request.headers.get("if-none-match");
  if (!header) return false;
  return header.split(",").some((part) => {
    const token = part.trim();
    return token === etag || token === `W/${etag}` || token === "*";
  });
}

export function publicCacheHeaders(etag: string, cacheControl: string): HeadersInit {
  return {
    "Cache-Control": cacheControl,
    ETag: etag,
    Vary: PUBLIC_CACHE_VARY,
    "Content-Type": "application/json",
  };
}

export function notModified(etag: string, cacheControl: string): NextResponse {
  return new NextResponse(null, {
    status: 304,
    headers: {
      "Cache-Control": cacheControl,
      ETag: etag,
      Vary: PUBLIC_CACHE_VARY,
    },
  });
}

export function jsonPublic(
  request: Request,
  body: unknown,
  cacheControl: string,
  options?: { etag?: string; maxBytes?: number },
): NextResponse {
  const json = JSON.stringify(body);
  if (options?.maxBytes != null && Buffer.byteLength(json) > options.maxBytes) {
    return new NextResponse(json, {
      status: 200,
      headers: {
        "Cache-Control": PRIVATE_NO_STORE,
        "Content-Type": "application/json",
      },
    });
  }
  const etag = options?.etag ?? strongEtag(json);
  if (ifNoneMatch(request, etag)) {
    return notModified(etag, cacheControl);
  }
  return new NextResponse(json, {
    status: 200,
    headers: publicCacheHeaders(etag, cacheControl),
  });
}
