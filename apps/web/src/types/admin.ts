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
