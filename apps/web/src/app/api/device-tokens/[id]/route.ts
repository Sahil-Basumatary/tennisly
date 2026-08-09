import { NextResponse } from "next/server";
import { auth } from "@clerk/nextjs/server";

type RouteContext = { params: Promise<{ id: string }> };

function notificationBase(): string {
  return (process.env.NOTIFICATION_SERVICE_URL ?? "http://localhost:18087").replace(/\/$/, "");
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { userId, getToken } = await auth();
  if (!userId) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  const token = await getToken();
  try {
    const headers: Record<string, string> = { "X-User-Id": userId };
    if (token) headers.Authorization = `Bearer ${token}`;
    const response = await fetch(
      `${notificationBase()}/api/notifications/me/device-tokens/${id}`,
      { method: "DELETE", cache: "no-store", headers },
    );
    if (!response.ok && response.status !== 204) {
      return NextResponse.json(
        { error: `notification-service ${response.status}` },
        { status: response.status },
      );
    }
    return new NextResponse(null, { status: 204 });
  } catch {
    return NextResponse.json({ error: "notification-service unreachable" }, { status: 502 });
  }
}
