"use client";

import { useCallback, useEffect, useState } from "react";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import type {
  AdminActiveFilter,
  AdminApiKey,
  AdminCreateApiKeyResponse,
  AdminPage,
} from "@/types/admin";

const ACTIVE_OPTIONS = [
  { id: "ALL" as const, label: "All" },
  { id: "ACTIVE" as const, label: "Active" },
  { id: "INACTIVE" as const, label: "Revoked" },
];

function formatInstant(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

export function AdminKeysPanel() {
  const [organizationId, setOrganizationId] = useState("");
  const [active, setActive] = useState<AdminActiveFilter>("ALL");
  const [page, setPage] = useState<AdminPage<AdminApiKey> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createName, setCreateName] = useState("");
  const [createOrgId, setCreateOrgId] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [plaintextKey, setPlaintextKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState<string | null>(null);
  const [revokingId, setRevokingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ active, page: "0", size: "25" });
    if (organizationId.trim()) params.set("organizationId", organizationId.trim());
    try {
      const response = await fetch(`/api/admin/api-keys?${params.toString()}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setPage((await response.json()) as AdminPage<AdminApiKey>);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load API keys");
      setPage(null);
    } finally {
      setLoading(false);
    }
  }, [active, organizationId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    setCreating(true);
    setCreateError(null);
    setPlaintextKey(null);
    setCopied(false);
    setCopyError(null);
    try {
      const response = await fetch("/api/admin/api-keys", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          organizationId: createOrgId.trim(),
          name: createName.trim(),
        }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Create failed (${response.status})`);
      }
      const data = (await response.json()) as AdminCreateApiKeyResponse;
      setPlaintextKey(data.plaintextKey);
      setCreateName("");
      setCreateOrgId("");
      await load();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : "Failed to create API key");
    } finally {
      setCreating(false);
    }
  }

  async function handleRevoke(id: string) {
    setRevokingId(id);
    setError(null);
    try {
      const response = await fetch(`/api/admin/api-keys/${id}/revoke`, { method: "POST" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Revoke failed (${response.status})`);
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to revoke API key");
    } finally {
      setRevokingId(null);
    }
  }

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), 2000);
    return () => window.clearTimeout(timer);
  }, [copied]);

  async function copyPlaintextKey() {
    if (!plaintextKey) return;
    try {
      await navigator.clipboard.writeText(plaintextKey);
      setCopied(true);
      setCopyError(null);
    } catch {
      setCopied(false);
      setCopyError("Clipboard blocked — select the key and copy it yourself.");
    }
  }

  function dismissPlaintextKey() {
    setPlaintextKey(null);
    setCopied(false);
    setCopyError(null);
  }

  return (
    <div className="space-y-4">
      {plaintextKey ? (
        <div className="border border-primary bg-primary/5 p-4 sm:p-5">
          <p className="font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-primary">
            Copy this key now — it will not be shown again
          </p>
          <p className="mt-2 max-w-3xl font-sans text-sm text-foreground">
            This is the only time Tennisly shows the full secret. Save it in a password manager or
            your team&apos;s secrets vault. We store a hash, not this string — if you lose it, revoke
            the key and issue a new one.
          </p>
          <p className="mt-3 break-all font-data text-[13px] tabular-nums">{plaintextKey}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => void copyPlaintextKey()}
              className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
            >
              {copied ? "Copied" : "Copy key"}
            </button>
            <button
              type="button"
              onClick={dismissPlaintextKey}
              className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide"
            >
              Dismiss
            </button>
            <span className="sr-only" role="status" aria-live="polite">
              {copied ? "API key copied to clipboard" : ""}
            </span>
          </div>
          {copyError ? (
            <p className="mt-2 font-sans text-sm text-destructive">{copyError}</p>
          ) : null}
        </div>
      ) : null}
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-3 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Issue key
        </h2>
        <p className="mb-4 max-w-3xl font-sans text-sm text-muted-foreground">
          The full key is shown once, right after you create it. Copy it immediately and store it
          somewhere durable. After you dismiss that banner, only the prefix stays in this table.
        </p>
        <form onSubmit={(event) => void handleCreate(event)} className="flex flex-wrap items-end gap-4">
          <label className="min-w-[280px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Organization ID
            </span>
            <input
              value={createOrgId}
              onChange={(event) => setCreateOrgId(event.target.value)}
              placeholder="UUID"
              required
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label className="min-w-[220px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Name
            </span>
            <input
              value={createName}
              onChange={(event) => setCreateName(event.target.value)}
              placeholder="Partner integration"
              required
              maxLength={100}
              className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
            />
          </label>
          <button
            type="submit"
            disabled={creating}
            className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
          >
            {creating ? "Creating…" : "Create key"}
          </button>
        </form>
        {createError ? (
          <p className="mt-3 font-sans text-sm text-destructive">{createError}</p>
        ) : null}
      </div>
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="flex flex-wrap items-end gap-4">
          <label className="min-w-[280px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Filter by organization
            </span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="Organization UUID (optional)"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <SegmentedControl
            label="Status"
            options={ACTIVE_OPTIONS}
            value={active}
            onChange={setActive}
          />
          <button
            type="button"
            onClick={() => void load()}
            className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
          >
            Apply
          </button>
        </div>
      </div>
      {error ? (
        <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
          {error}
        </p>
      ) : null}
      <div className="overflow-x-auto border border-hairline bg-white">
        <table className="min-w-full border-collapse font-sans text-[13px]">
          <thead>
            <tr className="border-b border-hairline bg-muted/30 text-left">
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Name</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Prefix</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Organization</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Scopes</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Status</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Created</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-3 py-6 text-muted-foreground">
                  Loading API keys…
                </td>
              </tr>
            ) : page && page.content.length > 0 ? (
              page.content.map((key) => (
                <tr key={key.id} className="border-b border-hairline hover:bg-muted/20">
                  <td className="px-3 py-2 font-semibold">{key.name}</td>
                  <td className="px-3 py-2 font-data text-[12px]">{key.keyPrefix}…</td>
                  <td className="px-3 py-2 font-data text-[11px]">{key.organizationId}</td>
                  <td className="px-3 py-2 font-data text-[12px]">{key.scopes.join(", ")}</td>
                  <td className="px-3 py-2">
                    <span
                      className={
                        key.active
                          ? "font-semibold text-primary"
                          : "font-semibold text-muted-foreground"
                      }
                    >
                      {key.active ? "Active" : "Revoked"}
                    </span>
                  </td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {formatInstant(key.createdAt)}
                  </td>
                  <td className="px-3 py-2">
                    {key.active ? (
                      <button
                        type="button"
                        disabled={revokingId === key.id}
                        onClick={() => void handleRevoke(key.id)}
                        className="font-sans text-[11px] font-semibold uppercase tracking-wide text-destructive disabled:opacity-60"
                      >
                        {revokingId === key.id ? "Revoking…" : "Revoke"}
                      </button>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={7} className="px-3 py-6 text-muted-foreground">
                  No API keys match this filter.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {page ? (
        <p className="font-sans text-[12px] text-muted-foreground">
          Showing {page.content.length} of{" "}
          <span className="font-data tabular-nums">{page.totalElements}</span> keys
        </p>
      ) : null}
    </div>
  );
}
