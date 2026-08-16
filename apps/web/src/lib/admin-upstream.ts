import type {
  AdminApiKey,
  AdminApiKeyQuery,
  AdminAuditLog,
  AdminAuditLogQuery,
  AdminCreateApiKeyPayload,
  AdminCreateApiKeyResponse,
  AdminHealthResponse,
  AdminListQuery,
  AdminOrganization,
  AdminOrgMember,
  AdminPage,
  AdminUpdateOrganizationPayload,
  AdminUpdateUserPayload,
  AdminUsageResponse,
  AdminUser,
  AdminWebhookEndpoint,
  AdminCreateWebhookPayload,
  AdminCreateWebhookResponse,
  AdminWebhookDelivery,
  AdminWebhookDeliveryPage,
  AdminWebhookDeliveryStatus,
} from "@/types/admin";

function userServiceBase(): string {
  const configured = process.env.USER_SERVICE_URL?.replace(/\/$/, "");
  if (configured) {
    return configured;
  }
  if (process.env.VERCEL) {
    throw new AdminUpstreamError("USER_SERVICE_URL is not set", 503);
  }
  return "http://localhost:8082";
}

export class AdminUpstreamError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "AdminUpstreamError";
  }
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new AdminUpstreamError(`user-service ${response.status}`, response.status);
  }
  return (await response.json()) as T;
}

function authHeaders(token: string | null, userId: string, roles: string): HeadersInit {
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
    "X-User-Id": userId,
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (roles) headers["X-User-Roles"] = roles;
  return headers;
}

async function upstreamFetch(path: string, init?: RequestInit): Promise<Response> {
  const url = `${userServiceBase()}${path}`;
  try {
    return await fetch(url, {
      cache: "no-store",
      ...init,
    });
  } catch (err) {
    if (err instanceof AdminUpstreamError) {
      throw err;
    }
    throw new AdminUpstreamError("user-service unreachable", 502);
  }
}

function queryString(params: Record<string, string | number | boolean | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

function activeParam(filter?: AdminListQuery["active"]): boolean | undefined {
  if (filter === "ACTIVE") return true;
  if (filter === "INACTIVE") return false;
  return undefined;
}

export async function fetchUpstreamAdminOrganizations(
  token: string | null,
  userId: string,
  roles: string,
  query?: AdminListQuery,
): Promise<AdminPage<AdminOrganization>> {
  const qs = queryString({
    q: query?.q,
    active: activeParam(query?.active),
    page: query?.page,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/users/admin/organizations${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminPage<AdminOrganization>>(response);
}

export async function fetchUpstreamAdminOrganization(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
): Promise<AdminOrganization> {
  const response = await upstreamFetch(`/api/users/admin/organizations/${id}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminOrganization>(response);
}

export async function updateUpstreamAdminOrganization(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
  payload: AdminUpdateOrganizationPayload,
): Promise<AdminOrganization> {
  const response = await upstreamFetch(`/api/users/admin/organizations/${id}`, {
    method: "PUT",
    headers: authHeaders(token, userId, roles),
    body: JSON.stringify(payload),
  });
  return readJson<AdminOrganization>(response);
}

export async function fetchUpstreamAdminUsers(
  token: string | null,
  userId: string,
  roles: string,
  query?: AdminListQuery,
): Promise<AdminPage<AdminUser>> {
  const qs = queryString({
    q: query?.q,
    active: activeParam(query?.active),
    page: query?.page,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/users/admin/users${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminPage<AdminUser>>(response);
}

export async function fetchUpstreamAdminUser(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
): Promise<AdminUser> {
  const response = await upstreamFetch(`/api/users/admin/users/${id}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminUser>(response);
}

export async function updateUpstreamAdminUser(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
  payload: AdminUpdateUserPayload,
): Promise<AdminUser> {
  const response = await upstreamFetch(`/api/users/admin/users/${id}`, {
    method: "PUT",
    headers: authHeaders(token, userId, roles),
    body: JSON.stringify(payload),
  });
  return readJson<AdminUser>(response);
}

export async function fetchUpstreamOrgMembers(
  token: string | null,
  userId: string,
  roles: string,
  orgId: string,
): Promise<AdminOrgMember[]> {
  const response = await upstreamFetch(`/api/users/organizations/${orgId}/members`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminOrgMember[]>(response);
}

export async function fetchUpstreamAdminApiKeys(
  token: string | null,
  userId: string,
  roles: string,
  query?: AdminApiKeyQuery,
): Promise<AdminPage<AdminApiKey>> {
  const qs = queryString({
    organizationId: query?.organizationId,
    active: activeParam(query?.active),
    page: query?.page,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/users/admin/api-keys${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminPage<AdminApiKey>>(response);
}

export async function createUpstreamAdminApiKey(
  token: string | null,
  userId: string,
  roles: string,
  payload: AdminCreateApiKeyPayload,
): Promise<AdminCreateApiKeyResponse> {
  const response = await upstreamFetch(`/api/users/admin/api-keys`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
    body: JSON.stringify(payload),
  });
  return readJson<AdminCreateApiKeyResponse>(response);
}

export async function revokeUpstreamAdminApiKey(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
): Promise<AdminApiKey> {
  const response = await upstreamFetch(`/api/users/admin/api-keys/${id}/revoke`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminApiKey>(response);
}

export async function fetchUpstreamAdminWebhooks(
  token: string | null,
  userId: string,
  roles: string,
  organizationId: string,
): Promise<AdminWebhookEndpoint[]> {
  const qs = queryString({ organizationId });
  const response = await upstreamFetch(`/api/users/admin/webhooks${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminWebhookEndpoint[]>(response);
}

export async function createUpstreamAdminWebhook(
  token: string | null,
  userId: string,
  roles: string,
  payload: AdminCreateWebhookPayload,
): Promise<AdminCreateWebhookResponse> {
  const response = await upstreamFetch(`/api/users/admin/webhooks`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
    body: JSON.stringify(payload),
  });
  return readJson<AdminCreateWebhookResponse>(response);
}

export async function revokeUpstreamAdminWebhook(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
  organizationId: string,
): Promise<AdminWebhookEndpoint> {
  const qs = queryString({ organizationId });
  const response = await upstreamFetch(`/api/users/admin/webhooks/${id}/revoke${qs}`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminWebhookEndpoint>(response);
}

export async function rotateUpstreamAdminWebhookSecret(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
  organizationId: string,
): Promise<AdminCreateWebhookResponse> {
  const qs = queryString({ organizationId });
  const response = await upstreamFetch(`/api/users/admin/webhooks/${id}/rotate-secret${qs}`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminCreateWebhookResponse>(response);
}

export async function testUpstreamAdminWebhook(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
  organizationId: string,
): Promise<void> {
  const qs = queryString({ organizationId });
  const response = await upstreamFetch(`/api/users/admin/webhooks/${id}/test${qs}`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
  });
  if (!response.ok) {
    throw new AdminUpstreamError(`user-service ${response.status}`, response.status);
  }
}

export async function fetchUpstreamAdminAuditLogs(
  token: string | null,
  userId: string,
  roles: string,
  query?: AdminAuditLogQuery,
): Promise<AdminPage<AdminAuditLog>> {
  const qs = queryString({
    q: query?.q,
    action: query?.action,
    organizationId: query?.organizationId,
    from: query?.from,
    to: query?.to,
    page: query?.page,
    size: query?.size,
  });
  const response = await upstreamFetch(`/api/users/admin/audit-logs${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminPage<AdminAuditLog>>(response);
}

export async function fetchUpstreamAdminUsage(
  token: string | null,
  userId: string,
  roles: string,
  organizationId: string,
  from?: string,
  to?: string,
): Promise<AdminUsageResponse> {
  const qs = queryString({ organizationId, from, to });
  const response = await upstreamFetch(`/api/users/admin/usage${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminUsageResponse>(response);
}

function notificationServiceBase(): string {
  const configured = process.env.NOTIFICATION_SERVICE_URL?.replace(/\/$/, "");
  if (configured) {
    return configured;
  }
  if (process.env.VERCEL) {
    throw new AdminUpstreamError("NOTIFICATION_SERVICE_URL is not set", 503);
  }
  return "http://localhost:18087";
}

async function notificationFetch(path: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(`${notificationServiceBase()}${path}`, {
      cache: "no-store",
      ...init,
    });
  } catch (err) {
    if (err instanceof AdminUpstreamError) {
      throw err;
    }
    throw new AdminUpstreamError("notification-service unreachable", 502);
  }
}

export async function fetchUpstreamAdminWebhookDeliveries(
  token: string | null,
  userId: string,
  roles: string,
  query?: {
    organizationId?: string;
    endpointId?: string;
    status?: AdminWebhookDeliveryStatus | "ALL";
    eventType?: string;
    page?: number;
    size?: number;
  },
): Promise<AdminWebhookDeliveryPage> {
  const qs = queryString({
    organizationId: query?.organizationId,
    endpointId: query?.endpointId,
    status: query?.status && query.status !== "ALL" ? query.status : undefined,
    eventType: query?.eventType,
    page: query?.page,
    size: query?.size,
  });
  const response = await notificationFetch(`/api/notifications/admin/deliveries${qs}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminWebhookDeliveryPage>(response);
}

export async function fetchUpstreamAdminWebhookDelivery(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
): Promise<AdminWebhookDelivery> {
  const response = await notificationFetch(`/api/notifications/admin/deliveries/${id}`, {
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminWebhookDelivery>(response);
}

export async function retryUpstreamAdminWebhookDelivery(
  token: string | null,
  userId: string,
  roles: string,
  id: string,
): Promise<AdminWebhookDelivery> {
  const response = await notificationFetch(`/api/notifications/admin/deliveries/${id}/retry`, {
    method: "POST",
    headers: authHeaders(token, userId, roles),
  });
  return readJson<AdminWebhookDelivery>(response);
}

type HealthTarget = {
  name: string;
  url: string;
};

function healthTargets(): HealthTarget[] {
  return [
    {
      name: "eureka",
      url: `${(process.env.EUREKA_URI ?? "http://localhost:18761/eureka").replace(/\/$/, "")}/apps`,
    },
    {
      name: "tennis-data",
      url: `${process.env.TENNIS_DATA_SERVICE_URL ?? "http://localhost:18083"}/actuator/health`,
    },
    {
      name: "match",
      url: `${process.env.MATCH_SERVICE_URL ?? "http://localhost:18084"}/actuator/health`,
    },
    {
      name: "replay",
      url: `${process.env.REPLAY_SERVICE_URL ?? "http://localhost:18085"}/actuator/health`,
    },
    {
      name: "analytics",
      url: `${process.env.ANALYTICS_SERVICE_URL ?? "http://localhost:18086"}/actuator/health`,
    },
    {
      name: "notification",
      url: `${process.env.NOTIFICATION_SERVICE_URL ?? "http://localhost:18087"}/actuator/health`,
    },
    {
      name: "user",
      url: `${userServiceBase()}/actuator/health`,
    },
  ];
}

export async function fetchAdminHealthSnapshot(): Promise<AdminHealthResponse> {
  const checks = await Promise.all(
    healthTargets().map(async (target) => {
      try {
        const response = await fetch(target.url, { cache: "no-store" });
        const up = response.ok;
        return {
          name: target.name,
          status: up ? ("UP" as const) : ("DOWN" as const),
          httpStatus: response.status,
        };
      } catch {
        return {
          name: target.name,
          status: "DOWN" as const,
          httpStatus: null,
        };
      }
    }),
  );
  return { services: checks };
}
