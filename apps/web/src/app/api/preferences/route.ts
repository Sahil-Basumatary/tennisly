import { NextResponse } from "next/server";
import {
  fetchUpstreamPreferences,
  noStoreJson,
  preferencesErrorResponse,
  requirePreferencesAuth,
  updateUpstreamPreferences,
  type UserPreferences,
} from "@/lib/preferences-upstream";

export async function GET() {
  const session = await requirePreferencesAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const data = await fetchUpstreamPreferences(session.token, session.userId);
    return noStoreJson(data);
  } catch (err) {
    return preferencesErrorResponse(err);
  }
}

export async function PUT(request: Request) {
  const session = await requirePreferencesAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const payload = (await request.json()) as Partial<{
      notificationsEnabled: boolean;
      emailNotifications: boolean;
      extraSettings: UserPreferences["extraSettings"];
    }>;
    const data = await updateUpstreamPreferences(session.token, session.userId, payload);
    return noStoreJson(data);
  } catch (err) {
    return preferencesErrorResponse(err);
  }
}
