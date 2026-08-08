import { NextResponse } from "next/server";
import {
  createUpstreamSavedView,
  fetchUpstreamSavedViews,
} from "@/lib/analytics-upstream";
import {
  analyticsErrorResponse,
  noStoreJson,
  requireAnalyticsAuth,
} from "@/lib/analytics-bff";
import type { CreateSavedViewPayload } from "@/types/analytics";

export async function GET() {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const views = await fetchUpstreamSavedViews(session.token, session.userId);
    return noStoreJson(views);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}

export async function POST(request: Request) {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  let payload: CreateSavedViewPayload;
  try {
    payload = (await request.json()) as CreateSavedViewPayload;
  } catch {
    return NextResponse.json({ error: "invalid json" }, { status: 400 });
  }
  try {
    const created = await createUpstreamSavedView(session.token, session.userId, payload);
    return noStoreJson(created, { status: 201 });
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
