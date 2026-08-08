"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import type { AdminUser } from "@/types/admin";

type AdminUserDetailProps = {
  userId: string;
};

export function AdminUserDetail({ userId }: AdminUserDetailProps) {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [active, setActive] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/admin/users/${userId}`, { cache: "no-store" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      const data = (await response.json()) as AdminUser;
      setUser(data);
      setDisplayName(data.displayName ?? "");
      setActive(data.active);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load user");
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function save() {
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const response = await fetch(`/api/admin/users/${userId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ displayName: displayName || null, active }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Save failed (${response.status})`);
      }
      const updated = (await response.json()) as AdminUser;
      setUser(updated);
      setMessage(updated.active ? "User reactivated." : "User deactivated.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save user");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <p className="font-sans text-sm text-muted-foreground">Loading user…</p>;
  }

  if (!user) {
    return (
      <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
        {error ?? "User not found."}
      </p>
    );
  }

  return (
    <div className="border border-hairline bg-white p-4 sm:p-5">
      <div className="mb-4 grid gap-3 sm:grid-cols-2">
        <div>
          <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Email
          </p>
          <p className="font-sans text-[14px] font-semibold">{user.email}</p>
        </div>
        <div>
          <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Clerk id
          </p>
          <p className="font-data text-[12px]">{user.clerkId}</p>
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <label>
          <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Display name
          </span>
          <input
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
          />
        </label>
        <label className="flex items-end gap-2 pb-2">
          <input
            id="user-active"
            type="checkbox"
            checked={active}
            onChange={(event) => setActive(event.target.checked)}
          />
          <span className="font-sans text-[13px] font-semibold">Active</span>
        </label>
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <button
          type="button"
          disabled={saving}
          onClick={() => void save()}
          className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
        >
          {saving ? "Saving…" : active ? "Save / reactivate" : "Deactivate user"}
        </button>
        <Link href="/admin/users" className="font-sans text-[13px] font-semibold text-primary hover:underline">
          Back to users
        </Link>
      </div>
      {message ? <p className="mt-3 font-sans text-sm text-primary">{message}</p> : null}
      {error ? <p className="mt-3 font-sans text-sm text-destructive">{error}</p> : null}
    </div>
  );
}
