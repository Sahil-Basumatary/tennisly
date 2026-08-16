import { NextResponse } from "next/server";
import { auth } from "@clerk/nextjs/server";

function notificationBase(): string {
  const configured = process.env.NOTIFICATION_SERVICE_URL?.replace(/\/$/, "");
  if (configured) {
    return configured;
  }
  if (process.env.VERCEL) {
    throw new Error("NOTIFICATION_SERVICE_URL is not set");
  }
  return "http://localhost:18087";
}

async function requireSession() {
  const { userId, getToken } = await auth();
  if (!userId) return null;
  return { userId, token: await getToken() };
}

function authHeaders(token: string | null, userId: string): HeadersInit {
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
    "X-User-Id": userId,
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

export async function GET() {
  const session = await requireSession();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const response = await fetch(`${notificationBase()}/api/notifications/me/device-tokens`, {
      cache: "no-store",
      headers: authHeaders(session.token, session.userId),
    });
    if (!response.ok) {
      return NextResponse.json(
        { error: `notification-service ${response.status}` },
        { status: response.status },
      );
    }
    return NextResponse.json(await response.json(), {
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json({ error: "notification-service unreachable" }, { status: 502 });
  }
}

export async function POST(request: Request) {
  const session = await requireSession();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const body = await request.text();
    const response = await fetch(`${notificationBase()}/api/notifications/me/device-tokens`, {
      method: "POST",
      cache: "no-store",
      headers: authHeaders(session.token, session.userId),
      body,
    });
    if (!response.ok) {
      return NextResponse.json(
        { error: `notification-service ${response.status}` },
        { status: response.status },
      );
    }
    return NextResponse.json(await response.json(), {
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json({ error: "notification-service unreachable" }, { status: 502 });
  }
}
