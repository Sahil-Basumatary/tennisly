import { NextResponse } from "next/server";

const NAMES = new Set(["LCP", "INP", "CLS", "TTFB"]);

export async function POST(request: Request) {
  let payload: { name?: string; value?: number; path?: string };
  try {
    payload = (await request.json()) as { name?: string; value?: number; path?: string };
  } catch {
    return NextResponse.json({ error: "invalid json" }, { status: 400 });
  }
  if (!payload.name || !NAMES.has(payload.name) || typeof payload.value !== "number") {
    return NextResponse.json({ error: "invalid metric" }, { status: 400 });
  }
  if (process.env.NODE_ENV === "development") {
    console.info("[vitals]", payload.name, Math.round(payload.value), payload.path);
  }
  return NextResponse.json({ ok: true });
}
