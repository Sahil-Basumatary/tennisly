"use client";

import { useCallback, useEffect, useState } from "react";
import type { AdminAuditLog, AdminPage } from "@/types/admin";

const ACTION_OPTIONS = [
  "",
  "ORG_UPDATE",
  "USER_UPDATE",
  "USER_DEACTIVATE",
  "API_KEY_CREATE",
  "API_KEY_REVOKE",
];

function formatInstant(value: string): string {
  return new Date(value).toLocaleString();
}

export function AdminAuditPanel() {
  const [q, setQ] = useState("");
  const [action, setAction] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState<AdminPage<AdminAuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ page: "0", size: "50" });
    if (q.trim()) params.set("q", q.trim());
    if (action) params.set("action", action);
    if (organizationId.trim()) params.set("organizationId", organizationId.trim());
    if (from) params.set("from", new Date(from).toISOString());
    if (to) params.set("to", new Date(to).toISOString());
    try {
      const response = await fetch(`/api/admin/audit-logs?${params.toString()}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setPage((await response.json()) as AdminPage<AdminAuditLog>);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load audit logs");
      setPage(null);
    } finally {
      setLoading(false);
    }
  }, [action, from, organizationId, q, to]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Search
            </span>
            <input
              value={q}
              onChange={(event) => setQ(event.target.value)}
              placeholder="Actor, email, resource ID"
              className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
            />
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Action
            </span>
            <select
              value={action}
              onChange={(event) => setAction(event.target.value)}
              className="w-full border border-hairline bg-white px-3 py-2 font-sans text-sm outline-none focus:border-primary"
            >
              {ACTION_OPTIONS.map((option) => (
                <option key={option || "all"} value={option}>
                  {option || "All actions"}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Organization ID
            </span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="UUID (optional)"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              From
            </span>
            <input
              type="datetime-local"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              To
            </span>
            <input
              type="datetime-local"
              value={to}
              onChange={(event) => setTo(event.target.value)}
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
        </div>
        <button
          type="button"
          onClick={() => void load()}
          className="mt-4 border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
        >
          Apply
        </button>
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
              <th className="px-3 py-2 font-bold uppercase tracking-wide">When</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Action</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Actor</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Resource</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Organization</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-muted-foreground">
                  Loading audit logs…
                </td>
              </tr>
            ) : page && page.content.length > 0 ? (
              page.content.map((entry) => (
                <tr key={entry.id} className="border-b border-hairline hover:bg-muted/20">
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums whitespace-nowrap">
                    {formatInstant(entry.createdAt)}
                  </td>
                  <td className="px-3 py-2 font-data text-[12px]">{entry.action}</td>
                  <td className="px-3 py-2">
                    <div className="font-data text-[12px]">{entry.actorClerkId}</div>
                    {entry.actorEmail ? (
                      <div className="text-[11px] text-muted-foreground">{entry.actorEmail}</div>
                    ) : null}
                  </td>
                  <td className="px-3 py-2">
                    <div className="font-data text-[12px]">{entry.resourceType}</div>
                    {entry.resourceId ? (
                      <div className="text-[11px] text-muted-foreground">{entry.resourceId}</div>
                    ) : null}
                  </td>
                  <td className="px-3 py-2 font-data text-[11px]">
                    {entry.organizationId ?? "—"}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-muted-foreground">
                  No audit entries match this filter.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {page ? (
        <p className="font-sans text-[12px] text-muted-foreground">
          Showing {page.content.length} of{" "}
          <span className="font-data tabular-nums">{page.totalElements}</span> entries
        </p>
      ) : null}
    </div>
  );
}
