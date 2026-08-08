"use client";

import { useCallback, useState } from "react";
import type { AdminUsageResponse } from "@/types/admin";

function formatDay(value: string): string {
  return new Date(`${value}T00:00:00Z`).toLocaleDateString();
}

export function AdminUsagePanel() {
  const [organizationId, setOrganizationId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [usage, setUsage] = useState<AdminUsageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!organizationId.trim()) {
      setError("Organization ID is required");
      setUsage(null);
      return;
    }
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ organizationId: organizationId.trim() });
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    try {
      const response = await fetch(`/api/admin/usage?${params.toString()}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setUsage((await response.json()) as AdminUsageResponse);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load usage");
      setUsage(null);
    } finally {
      setLoading(false);
    }
  }, [from, organizationId, to]);

  const totals = usage ? Object.entries(usage.totalsByMetric) : [];

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="flex flex-wrap items-end gap-4">
          <label className="min-w-[280px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Organization ID
            </span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="UUID"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              From
            </span>
            <input
              type="date"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
              className="border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              To
            </span>
            <input
              type="date"
              value={to}
              onChange={(event) => setTo(event.target.value)}
              className="border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading}
            className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
          >
            {loading ? "Loading…" : "Load usage"}
          </button>
        </div>
      </div>
      {error ? (
        <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
          {error}
        </p>
      ) : null}
      {totals.length > 0 ? (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {totals.map(([metric, count]) => (
            <div key={metric} className="border border-hairline bg-white p-4">
              <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                {metric}
              </p>
              <p className="mt-1 font-data text-2xl tabular-nums">{count.toLocaleString()}</p>
            </div>
          ))}
        </div>
      ) : null}
      <div className="overflow-x-auto border border-hairline bg-white">
        <table className="min-w-full border-collapse font-sans text-[13px]">
          <thead>
            <tr className="border-b border-hairline bg-muted/30 text-left">
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Day</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Metric</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Count</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={3} className="px-3 py-6 text-muted-foreground">
                  Loading usage…
                </td>
              </tr>
            ) : usage && usage.daily.length > 0 ? (
              usage.daily.map((row) => (
                <tr
                  key={`${row.day}-${row.metric}`}
                  className="border-b border-hairline hover:bg-muted/20"
                >
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {formatDay(row.day)}
                  </td>
                  <td className="px-3 py-2 font-data text-[12px]">{row.metric}</td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {row.count.toLocaleString()}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={3} className="px-3 py-6 text-muted-foreground">
                  {usage ? "No usage rows for this range." : "Enter an organization ID to load usage."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
