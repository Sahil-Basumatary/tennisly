import { NextResponse } from "next/server";
import {
  deleteUpstreamSavedView,
  fetchUpstreamSavedView,
  updateUpstreamSavedView,
} from "@/lib/analytics-upstream";
import {
  analyticsErrorResponse,
  noStoreJson,
  requireAnalyticsAuth,
} from "@/lib/analytics-bff";
import type { UpdateSavedViewPayload } from "@/types/analytics";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  try {
    const view = await fetchUpstreamSavedView(session.token, session.userId, id);
    return noStoreJson(view);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}

export async function PUT(request: Request, context: RouteContext) {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  let payload: UpdateSavedViewPayload;
  try {
    payload = (await request.json()) as UpdateSavedViewPayload;
  } catch {
    return NextResponse.json({ error: "invalid json" }, { status: 400 });
  }
  try {
    const updated = await updateUpstreamSavedView(session.token, session.userId, id, payload);
    return noStoreJson(updated);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}

export async function DELETE(_request: Request, context: RouteContext) {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  try {
    await deleteUpstreamSavedView(session.token, session.userId, id);
    return new NextResponse(null, { status: 204 });
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
