import { NextResponse } from "next/server";
import { auth } from "@clerk/nextjs/server";

export class PreferencesUpstreamError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "PreferencesUpstreamError";
  }
}

export async function requirePreferencesAuth() {
  const { userId, getToken } = await auth();
  if (!userId) return null;
  const token = await getToken();
  return { userId, token };
}

export function preferencesErrorResponse(err: unknown) {
  const status = err instanceof PreferencesUpstreamError ? (err.status ?? 502) : 502;
  const message = err instanceof Error ? err.message : "preferences upstream unavailable";
  return NextResponse.json({ error: message }, { status });
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

function userServiceBase(): string {
  return (process.env.USER_SERVICE_URL ?? "http://localhost:8082").replace(/\/$/, "");
}

export type UserPreferences = {
  id: string;
  theme: string;
  notificationsEnabled: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
  favoriteSurface: string | null;
  locale: string;
  extraSettings: {
    emailCategories?: {
      welcome?: boolean;
      apiKeyRevoked?: boolean;
      webhookFailed?: boolean;
    };
    pushCategories?: {
      welcome?: boolean;
      apiKeyRevoked?: boolean;
      webhookFailed?: boolean;
    };
    [key: string]: unknown;
  };
};

export async function fetchUpstreamPreferences(
  token: string | null,
  userId: string,
): Promise<UserPreferences> {
  const response = await fetch(`${userServiceBase()}/api/users/me/preferences`, {
    cache: "no-store",
    headers: {
      Accept: "application/json",
      "X-User-Id": userId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!response.ok) {
    throw new PreferencesUpstreamError(`user-service ${response.status}`, response.status);
  }
  return (await response.json()) as UserPreferences;
}

export async function updateUpstreamPreferences(
  token: string | null,
  userId: string,
  payload: Partial<{
    notificationsEnabled: boolean;
    emailNotifications: boolean;
    pushNotifications: boolean;
    extraSettings: UserPreferences["extraSettings"];
  }>,
): Promise<UserPreferences> {
  const response = await fetch(`${userServiceBase()}/api/users/me/preferences`, {
    method: "PUT",
    cache: "no-store",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-User-Id": userId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new PreferencesUpstreamError(`user-service ${response.status}`, response.status);
  }
  return (await response.json()) as UserPreferences;
}
