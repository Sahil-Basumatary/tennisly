"use client";

import { useCallback, useEffect, useState } from "react";
import type { UserPreferences } from "@/lib/preferences-upstream";

type CategoryFlags = {
  welcome: boolean;
  apiKeyRevoked: boolean;
  webhookFailed: boolean;
};

type DeviceToken = {
  id: string;
  platform: "WEB" | "IOS" | "ANDROID";
  active: boolean;
  tokenSuffix: string;
  lastSeenAt: string;
  createdAt: string;
};

const DEFAULT_CATEGORIES: CategoryFlags = {
  welcome: true,
  apiKeyRevoked: true,
  webhookFailed: true,
};

function readEmailCategories(prefs: UserPreferences | null): CategoryFlags {
  const raw = prefs?.extraSettings?.emailCategories;
  return {
    welcome: raw?.welcome ?? true,
    apiKeyRevoked: raw?.apiKeyRevoked ?? true,
    webhookFailed: raw?.webhookFailed ?? true,
  };
}

function readPushCategories(prefs: UserPreferences | null): CategoryFlags {
  const raw = prefs?.extraSettings?.pushCategories;
  return {
    welcome: raw?.welcome ?? true,
    apiKeyRevoked: raw?.apiKeyRevoked ?? true,
    webhookFailed: raw?.webhookFailed ?? true,
  };
}

export function NotificationSettingsPanel() {
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);
  const [emailCategories, setEmailCategories] = useState<CategoryFlags>(DEFAULT_CATEGORIES);
  const [pushCategories, setPushCategories] = useState<CategoryFlags>(DEFAULT_CATEGORIES);
  const [tokens, setTokens] = useState<DeviceToken[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const loadTokens = useCallback(async () => {
    const response = await fetch("/api/device-tokens", { cache: "no-store" });
    if (!response.ok) return;
    setTokens((await response.json()) as DeviceToken[]);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch("/api/preferences", { cache: "no-store" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Load failed (${response.status})`);
      }
      const data = (await response.json()) as UserPreferences;
      setPrefs({
        ...data,
        pushNotifications: data.pushNotifications ?? true,
      });
      setEmailCategories(readEmailCategories(data));
      setPushCategories(readPushCategories(data));
      await loadTokens();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load preferences");
    } finally {
      setLoading(false);
    }
  }, [loadTokens]);

  useEffect(() => {
    void load();
  }, [load]);

  async function save() {
    if (!prefs) return;
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      const response = await fetch("/api/preferences", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          notificationsEnabled: prefs.notificationsEnabled,
          emailNotifications: prefs.emailNotifications,
          pushNotifications: prefs.pushNotifications,
          extraSettings: {
            ...prefs.extraSettings,
            emailCategories,
            pushCategories,
          },
        }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Save failed (${response.status})`);
      }
      const data = (await response.json()) as UserPreferences;
      setPrefs(data);
      setEmailCategories(readEmailCategories(data));
      setPushCategories(readPushCategories(data));
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save preferences");
    } finally {
      setSaving(false);
    }
  }

  async function registerDemoToken() {
    setRegistering(true);
    setError(null);
    try {
      const demoToken = `web-demo-${crypto.randomUUID()}`;
      const response = await fetch("/api/device-tokens", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: demoToken, platform: "WEB" }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Register failed (${response.status})`);
      }
      await loadTokens();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to register device token");
    } finally {
      setRegistering(false);
    }
  }

  async function deactivateToken(id: string) {
    setError(null);
    try {
      const response = await fetch(`/api/device-tokens/${id}`, { method: "DELETE" });
      if (!response.ok && response.status !== 204) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Deactivate failed (${response.status})`);
      }
      await loadTokens();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to deactivate token");
    }
  }

  if (loading) {
    return <p className="font-sans text-sm text-muted-foreground">Loading notification settings…</p>;
  }

  if (!prefs) {
    return (
      <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
        {error ?? "Preferences unavailable"}
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-4 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Master switches
        </h2>
        <div className="space-y-3">
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={prefs.notificationsEnabled}
              onChange={(event) =>
                setPrefs({ ...prefs, notificationsEnabled: event.target.checked })
              }
            />
            All notifications enabled
          </label>
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={prefs.emailNotifications}
              onChange={(event) =>
                setPrefs({ ...prefs, emailNotifications: event.target.checked })
              }
            />
            Email notifications enabled
          </label>
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={prefs.pushNotifications}
              onChange={(event) =>
                setPrefs({ ...prefs, pushNotifications: event.target.checked })
              }
            />
            Push notifications enabled
          </label>
        </div>
      </div>
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-4 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Email categories
        </h2>
        <CategoryToggles value={emailCategories} onChange={setEmailCategories} />
      </div>
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-4 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Push categories
        </h2>
        <CategoryToggles value={pushCategories} onChange={setPushCategories} />
      </div>
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Device tokens
          </h2>
          <button
            type="button"
            disabled={registering}
            onClick={() => void registerDemoToken()}
            className="border border-hairline bg-white px-3 py-1.5 font-sans text-[11px] font-semibold uppercase tracking-wide disabled:opacity-60"
          >
            {registering ? "Registering…" : "Register demo WEB token"}
          </button>
        </div>
        {tokens.length === 0 ? (
          <p className="font-sans text-sm text-muted-foreground">
            No devices registered. Register a demo token to exercise the push pipeline locally
            (logging provider).
          </p>
        ) : (
          <ul className="space-y-2">
            {tokens.map((token) => (
              <li
                key={token.id}
                className="flex flex-wrap items-center justify-between gap-3 border border-hairline px-3 py-2"
              >
                <div>
                  <p className="font-data text-[12px]">
                    {token.platform} · {token.tokenSuffix} ·{" "}
                    {token.active ? "active" : "inactive"}
                  </p>
                </div>
                {token.active ? (
                  <button
                    type="button"
                    onClick={() => void deactivateToken(token.id)}
                    className="font-sans text-[11px] font-semibold uppercase tracking-wide text-destructive"
                  >
                    Deactivate
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>
      {error ? <p className="font-sans text-sm text-destructive">{error}</p> : null}
      {saved ? (
        <p className="font-sans text-sm text-primary">Saved. Changes apply to future notifications.</p>
      ) : null}
      <button
        type="button"
        disabled={saving}
        onClick={() => void save()}
        className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
      >
        {saving ? "Saving…" : "Save preferences"}
      </button>
    </div>
  );
}

function CategoryToggles({
  value,
  onChange,
}: {
  value: CategoryFlags;
  onChange: (next: CategoryFlags) => void;
}) {
  return (
    <div className="space-y-3">
      <label className="flex items-center gap-3 font-sans text-sm">
        <input
          type="checkbox"
          checked={value.welcome}
          onChange={(event) => onChange({ ...value, welcome: event.target.checked })}
        />
        Welcome on signup
      </label>
      <label className="flex items-center gap-3 font-sans text-sm">
        <input
          type="checkbox"
          checked={value.apiKeyRevoked}
          onChange={(event) => onChange({ ...value, apiKeyRevoked: event.target.checked })}
        />
        API key revoked alerts
      </label>
      <label className="flex items-center gap-3 font-sans text-sm">
        <input
          type="checkbox"
          checked={value.webhookFailed}
          onChange={(event) => onChange({ ...value, webhookFailed: event.target.checked })}
        />
        Webhook delivery exhausted (DEAD) alerts
      </label>
    </div>
  );
}
