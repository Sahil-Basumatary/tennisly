export type PlanTier = "FREE" | "BASIC" | "PRO" | "ENTERPRISE";

export type AdminOrganization = {
  id: string;
  clerkOrgId: string;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  website: string | null;
  planTier: PlanTier;
  maxMembers: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminUser = {
  id: string;
  clerkId: string;
  email: string;
  displayName: string | null;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  country: string | null;
  timezone: string | null;
  bio: string | null;
  avatarUrl: string | null;
  skillLevel: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminPage<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminActiveFilter = "ALL" | "ACTIVE" | "INACTIVE";

export type AdminListQuery = {
  q?: string;
  active?: AdminActiveFilter;
  page?: number;
  size?: number;
};

export type AdminUpdateOrganizationPayload = {
  name?: string;
  description?: string | null;
  logoUrl?: string | null;
  website?: string | null;
  planTier?: PlanTier;
  maxMembers?: number;
  active?: boolean;
};

export type AdminUpdateUserPayload = {
  displayName?: string | null;
  active?: boolean;
};

export type AdminOrgMember = {
  id: string;
  userId: string;
  displayName: string | null;
  email: string;
  role: string;
  joinedAt: string;
};

export type AdminServiceHealth = {
  name: string;
  status: "UP" | "DOWN" | "UNKNOWN";
  httpStatus: number | null;
};

export type AdminHealthResponse = {
  services: AdminServiceHealth[];
};

export type AdminApiKey = {
  id: string;
  organizationId: string;
  name: string;
  keyPrefix: string;
  scopes: string[];
  active: boolean;
  lastUsedAt: string | null;
  expiresAt: string | null;
  createdByClerkId: string;
  revokedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminCreateApiKeyPayload = {
  organizationId: string;
  name: string;
  scopes?: string[];
  expiresAt?: string | null;
};

export type AdminCreateApiKeyResponse = {
  key: AdminApiKey;
  plaintextKey: string;
};

export type AdminAuditLog = {
  id: string;
  actorClerkId: string;
  actorEmail: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  organizationId: string | null;
  metadata: Record<string, unknown>;
  ipAddress: string | null;
  createdAt: string;
};

export type AdminAuditLogQuery = {
  q?: string;
  action?: string;
  organizationId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

export type AdminUsageDaily = {
  organizationId: string;
  metric: string;
  day: string;
  count: number;
};

export type AdminUsageResponse = {
  daily: AdminUsageDaily[];
  totalsByMetric: Record<string, number>;
};

export type AdminApiKeyQuery = {
  organizationId?: string;
  active?: AdminActiveFilter;
  page?: number;
  size?: number;
};

export type AdminWebhookEndpoint = {
  id: string;
  organizationId: string;
  name: string;
  targetUrl: string;
  secretPrefix: string;
  eventTypes: string[];
  active: boolean;
  description: string | null;
  createdByClerkId: string;
  revokedAt: string | null;
  lastDeliveryAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminCreateWebhookPayload = {
  organizationId: string;
  name: string;
  targetUrl: string;
  eventTypes: string[];
  description?: string | null;
};

export type AdminCreateWebhookResponse = {
  endpoint: AdminWebhookEndpoint;
  plaintextSecret: string;
};

export type AdminWebhookDeliveryStatus = "PENDING" | "SUCCESS" | "FAILED" | "DEAD";

export type AdminWebhookDelivery = {
  id: string;
  endpointId: string;
  organizationId: string;
  eventId: string;
  eventType: string;
  status: AdminWebhookDeliveryStatus;
  attemptCount: number;
  maxAttempts: number;
  nextAttemptAt: string | null;
  lastHttpStatus: number | null;
  lastError: string | null;
  responseMs: number | null;
  createdAt: string;
  updatedAt: string;
  deliveredAt: string | null;
  payload: string | null;
};

export type AdminWebhookDeliveryPage = {
  content: AdminWebhookDelivery[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
