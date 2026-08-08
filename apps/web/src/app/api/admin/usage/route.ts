import { NextResponse } from "next/server";
import { fetchUpstreamAdminUsage } from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";

export async function GET(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { searchParams } = new URL(request.url);
  const organizationId = searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json({ error: "organizationId required" }, { status: 400 });
  }
  const from = searchParams.get("from") ?? undefined;
  const to = searchParams.get("to") ?? undefined;
  try {
    const data = await fetchUpstreamAdminUsage(
      session.token,
      session.userId,
      session.roles,
      organizationId,
      from,
      to,
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
