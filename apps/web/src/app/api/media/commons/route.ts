import { NextResponse } from "next/server";
import {
  commonsImageContentType,
  fetchAllowlistedCommons,
  parseCommonsPhotoUrl,
} from "@/lib/commons-photo";

const MAX_BYTES = 1_500_000;

export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("u");
  if (!raw) {
    return new NextResponse("Missing photo", { status: 400 });
  }
  const target = parseCommonsPhotoUrl(raw);
  if (!target) {
    return new NextResponse("Unsupported photo", { status: 400 });
  }
  const upstream = await fetchAllowlistedCommons(target);
  if (!upstream?.ok) {
    return new NextResponse("Photo unavailable", { status: 502 });
  }
  const bytes = new Uint8Array(await upstream.arrayBuffer());
  if (bytes.byteLength === 0 || bytes.byteLength > MAX_BYTES) {
    return new NextResponse("Photo unavailable", { status: 502 });
  }
  const contentType = commonsImageContentType(
    upstream.headers.get("content-type") ?? "",
    bytes,
  );
  if (!contentType) {
    return new NextResponse("Unsupported photo", { status: 415 });
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
