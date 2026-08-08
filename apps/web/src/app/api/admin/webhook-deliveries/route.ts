import { NextResponse } from "next/server";
import { fetchUpstreamAdminWebhookDeliveries } from "@/lib/admin-upstream";
import { adminErrorResponse, noStoreJson, requireAdminAuth } from "@/lib/admin-bff";
import type { AdminWebhookDeliveryStatus } from "@/types/admin";

export async function GET(request: Request) {
  const session = await requireAdminAuth();
  if (!session) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  const { searchParams } = new URL(request.url);
  const status = searchParams.get("status") as AdminWebhookDeliveryStatus | "ALL" | null;
  const page = Number(searchParams.get("page") ?? "0");
  const size = Number(searchParams.get("size") ?? "25");
  try {
    const data = await fetchUpstreamAdminWebhookDeliveries(
      session.token,
      session.userId,
      session.roles,
      {
        organizationId: searchParams.get("organizationId") ?? undefined,
        endpointId: searchParams.get("endpointId") ?? undefined,
        status: status ?? "ALL",
        eventType: searchParams.get("eventType") ?? undefined,
        page: Number.isFinite(page) ? page : 0,
        size: Number.isFinite(size) ? size : 25,
      },
    );
    return noStoreJson(data);
  } catch (err) {
    return adminErrorResponse(err);
  }
}
