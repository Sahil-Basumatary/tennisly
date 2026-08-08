import { NextResponse } from "next/server";
import { auth } from "@clerk/nextjs/server";
import { AnalyticsUpstreamError } from "@/lib/analytics-upstream";

export function analyticsErrorResponse(err: unknown, fallback = "analytics-service unavailable") {
  const status = err instanceof AnalyticsUpstreamError ? (err.status ?? 502) : 502;
  return NextResponse.json({ error: fallback }, { status });
}

export async function requireAnalyticsAuth() {
  const { userId, getToken } = await auth();
  if (!userId) return null;
  const token = await getToken();
  return { userId, token };
}

export function noStoreJson<T>(data: T, init?: ResponseInit) {
  return NextResponse.json(data, {
    ...init,
    headers: {
      "Cache-Control": "no-store",
      ...(init?.headers ?? {}),
    },
  });
}
