import { NextResponse } from "next/server";
import {
  createUpstreamAdminWebhook,
  fetchUpstreamAdminWebhooks,
} from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";
import type { AdminCreateWebhookPayload } from "@/types/admin";

export async function GET(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { searchParams } = new URL(request.url);
  const organizationId = searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json({ error: "organizationId is required" }, { status: 400 });
  }
  try {
    const data = await fetchUpstreamAdminWebhooks(
      session.token,
      session.userId,
      session.roles,
      organizationId,
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}

export async function POST(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  try {
    const payload = (await request.json()) as AdminCreateWebhookPayload;
    const data = await createUpstreamAdminWebhook(
      session.token,
      session.userId,
      session.roles,
      payload,
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
