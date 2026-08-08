import { NextResponse } from "next/server";
import { favoriteUpstreamSavedView } from "@/lib/analytics-upstream";
import {
  analyticsErrorResponse,
  noStoreJson,
  requireAnalyticsAuth,
} from "@/lib/analytics-bff";

type RouteContext = { params: Promise<{ id: string }> };

export async function PATCH(request: Request, context: RouteContext) {
  const session = await requireAnalyticsAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  let favorite: boolean | undefined;
  try {
    const body = (await request.json()) as { favorite?: boolean };
    favorite = body.favorite;
  } catch {
    return NextResponse.json({ error: "invalid json" }, { status: 400 });
  }
  if (typeof favorite !== "boolean") {
    return NextResponse.json({ error: "favorite is required" }, { status: 400 });
  }
  try {
    const view = await favoriteUpstreamSavedView(session.token, session.userId, id, favorite);
    return noStoreJson(view);
  } catch (err) {
    return analyticsErrorResponse(err);
  }
}
