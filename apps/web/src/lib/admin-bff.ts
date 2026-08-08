import { NextResponse } from "next/server";
import { auth } from "@clerk/nextjs/server";
import { AdminUpstreamError } from "@/lib/admin-upstream";

export function adminErrorResponse(err: unknown, fallback = "admin upstream unavailable") {
  const status = err instanceof AdminUpstreamError ? (err.status ?? 502) : 502;
  const message =
    status === 403 ? "Admin access required" : err instanceof Error ? err.message : fallback;
  return NextResponse.json({ error: message }, { status });
}

export async function requireAdminAuth() {
  const { userId, getToken, sessionClaims } = await auth();
  if (!userId) return null;
  const token = await getToken();
  return { userId, token, roles: resolveRoles(sessionClaims) };
}

export function noStoreJson<T>(data: T, init?: ResponseInit) {
  return NextResponse.json(data, {
    ...init,
    headers: {
      "Cache-Control": "no-store",
      ...(init?.headers ?? {}),
    },
  });
}

function resolveRoles(sessionClaims: Record<string, unknown> | null | undefined): string {
  if (!sessionClaims) return "";
  const roles = sessionClaims.roles;
  if (Array.isArray(roles)) {
    return roles.map(String).join(",");
  }
  if (typeof roles === "string" && roles.trim()) {
    return roles;
  }
  const role = sessionClaims.role;
  if (typeof role === "string" && role.trim()) {
    return role;
  }
  const metadata = sessionClaims.metadata;
  if (metadata && typeof metadata === "object" && metadata !== null) {
    const metaRole = (metadata as Record<string, unknown>).role;
    if (typeof metaRole === "string" && metaRole.trim()) {
      return metaRole;
    }
  }
  const publicMetadata = sessionClaims.public_metadata;
  if (publicMetadata && typeof publicMetadata === "object" && publicMetadata !== null) {
    const metaRole = (publicMetadata as Record<string, unknown>).role;
    if (typeof metaRole === "string" && metaRole.trim()) {
      return metaRole;
    }
  }
  return "";
}
