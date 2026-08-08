import { NextResponse } from "next/server";
import { fetchUpstreamAdminAuditLogs } from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";

export async function GET(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { searchParams } = new URL(request.url);
  const q = searchParams.get("q") ?? undefined;
  const action = searchParams.get("action") ?? undefined;
  const organizationId = searchParams.get("organizationId") ?? undefined;
  const from = searchParams.get("from") ?? undefined;
  const to = searchParams.get("to") ?? undefined;
  const page = Number(searchParams.get("page") ?? "0");
  const size = Number(searchParams.get("size") ?? "25");
  try {
    const data = await fetchUpstreamAdminAuditLogs(session.token, session.userId, session.roles, {
      q,
      action,
      organizationId,
      from,
      to,
      page: Number.isFinite(page) ? page : 0,
      size: Number.isFinite(size) ? size : 25,
    });
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
