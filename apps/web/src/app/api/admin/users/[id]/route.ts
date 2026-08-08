import { NextResponse } from "next/server";
import { fetchUpstreamAdminUser, updateUpstreamAdminUser } from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";
import type { AdminUpdateUserPayload } from "@/types/admin";

type RouteContext = {
  params: Promise<{ id: string }>;
};

export async function GET(_request: Request, context: RouteContext) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  try {
    const data = await fetchUpstreamAdminUser(session.token, session.userId, session.roles, id);
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}

export async function PUT(request: Request, context: RouteContext) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  let payload: AdminUpdateUserPayload;
  try {
    payload = (await request.json()) as AdminUpdateUserPayload;
  } catch {
    return NextResponse.json({ error: "invalid json" }, { status: 400 });
  }
  try {
    const data = await updateUpstreamAdminUser(
      session.token,
      session.userId,
      session.roles,
      id,
      payload,
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
