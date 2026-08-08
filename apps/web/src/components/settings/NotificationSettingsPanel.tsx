"use client";

import { useCallback, useEffect, useState } from "react";
import type { UserPreferences } from "@/lib/preferences-upstream";

type EmailCategories = {
  welcome: boolean;
  apiKeyRevoked: boolean;
  webhookFailed: boolean;
};

const DEFAULT_CATEGORIES: EmailCategories = {
  welcome: true,
  apiKeyRevoked: true,
  webhookFailed: true,
};

function readCategories(prefs: UserPreferences | null): EmailCategories {
  const raw = prefs?.extraSettings?.emailCategories;
  return {
    welcome: raw?.welcome ?? true,
    apiKeyRevoked: raw?.apiKeyRevoked ?? true,
    webhookFailed: raw?.webhookFailed ?? true,
  };
}

export function NotificationSettingsPanel() {
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);
  const [categories, setCategories] = useState<EmailCategories>(DEFAULT_CATEGORIES);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

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
      setPrefs(data);
      setCategories(readCategories(data));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load preferences");
    } finally {
      setLoading(false);
    }
  }, []);

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
          extraSettings: {
            ...prefs.extraSettings,
            emailCategories: categories,
          },
        }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Save failed (${response.status})`);
      }
      const data = (await response.json()) as UserPreferences;
      setPrefs(data);
      setCategories(readCategories(data));
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save preferences");
    } finally {
      setSaving(false);
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
        </div>
      </div>
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-4 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Email categories
        </h2>
        <div className="space-y-3">
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={categories.welcome}
              onChange={(event) =>
                setCategories({ ...categories, welcome: event.target.checked })
              }
            />
            Welcome email on signup
          </label>
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={categories.apiKeyRevoked}
              onChange={(event) =>
                setCategories({ ...categories, apiKeyRevoked: event.target.checked })
              }
            />
            API key revoked alerts
          </label>
          <label className="flex items-center gap-3 font-sans text-sm">
            <input
              type="checkbox"
              checked={categories.webhookFailed}
              onChange={(event) =>
                setCategories({ ...categories, webhookFailed: event.target.checked })
              }
            />
            Webhook delivery exhausted (DEAD) alerts
          </label>
        </div>
      </div>
      {error ? <p className="font-sans text-sm text-destructive">{error}</p> : null}
      {saved ? (
        <p className="font-sans text-sm text-primary">Saved. Changes apply to future emails.</p>
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
