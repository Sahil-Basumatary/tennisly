import { NextResponse } from "next/server";
import { revokeUpstreamAdminApiKey } from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";

type RouteContext = { params: Promise<{ id: string }> };

export async function POST(_request: Request, context: RouteContext) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  try {
    const data = await revokeUpstreamAdminApiKey(
      session.token,
      session.userId,
      session.roles,
      id,
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
