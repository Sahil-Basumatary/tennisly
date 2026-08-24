import { NextResponse } from "next/server";
import { parseCommonsPhotoUrl, WIKIMEDIA_USER_AGENT } from "@/lib/commons-photo";

const MAX_BYTES = 1_500_000;
const ALLOWED_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("u");
  if (!raw) {
    return new NextResponse("Missing photo", { status: 400 });
  }
  const target = parseCommonsPhotoUrl(raw);
  if (!target) {
    return new NextResponse("Unsupported photo", { status: 400 });
  }
  let upstream: Response;
  try {
    upstream = await fetch(target, {
      headers: {
        Accept: "image/jpeg,image/png,image/webp",
        "User-Agent": WIKIMEDIA_USER_AGENT,
        "Api-User-Agent": WIKIMEDIA_USER_AGENT,
      },
      redirect: "manual",
      signal: AbortSignal.timeout(4000),
    });
  } catch {
    return new NextResponse("Photo unavailable", { status: 502 });
  }
  if (!upstream.ok) {
    return new NextResponse("Photo unavailable", { status: 502 });
  }
  const contentType = (upstream.headers.get("content-type") ?? "")
    .split(";")[0]
    .trim()
    .toLowerCase();
  if (!ALLOWED_TYPES.has(contentType)) {
    return new NextResponse("Unsupported photo", { status: 415 });
  }
  const bytes = new Uint8Array(await upstream.arrayBuffer());
  if (bytes.byteLength === 0 || bytes.byteLength > MAX_BYTES) {
    return new NextResponse("Photo unavailable", { status: 502 });
  }
  return new NextResponse(bytes, {
    status: 200,
    headers: {
      "Content-Type": contentType,
      "Cache-Control": "public, max-age=86400, s-maxage=604800, stale-while-revalidate=86400",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
