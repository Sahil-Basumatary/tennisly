import { NextResponse } from "next/server";
import { testUpstreamAdminWebhook } from "@/lib/admin-upstream";
import { adminErrorResponse, requireAdminAuth } from "@/lib/admin-bff";

type RouteContext = { params: Promise<{ id: string }> };

export async function POST(request: Request, context: RouteContext) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { id } = await context.params;
  const organizationId = new URL(request.url).searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json({ error: "organizationId is required" }, { status: 400 });
  }
  try {
    await testUpstreamAdminWebhook(
      session.token,
      session.userId,
      session.roles,
      id,
      organizationId,
    );
    return new NextResponse(null, { status: 202 });
  } catch (err) {
    return adminErrorResponse(err);
  }
}
