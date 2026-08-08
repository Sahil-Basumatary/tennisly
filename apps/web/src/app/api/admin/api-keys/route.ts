import { NextResponse } from "next/server";
import {
  createUpstreamAdminApiKey,
  fetchUpstreamAdminApiKeys,
} from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";
import type { AdminActiveFilter, AdminCreateApiKeyPayload } from "@/types/admin";

export async function GET(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { searchParams } = new URL(request.url);
  const active = searchParams.get("active") as AdminActiveFilter | null;
  const organizationId = searchParams.get("organizationId") ?? undefined;
  const page = Number(searchParams.get("page") ?? "0");
  const size = Number(searchParams.get("size") ?? "20");
  try {
    const data = await fetchUpstreamAdminApiKeys(session.token, session.userId, session.roles, {
      organizationId,
      active: active ?? "ALL",
      page: Number.isFinite(page) ? page : 0,
      size: Number.isFinite(size) ? size : 20,
    });
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
    const payload = (await request.json()) as AdminCreateApiKeyPayload;
    const data = await createUpstreamAdminApiKey(
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
