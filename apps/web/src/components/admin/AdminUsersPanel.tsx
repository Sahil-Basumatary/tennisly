"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import type { AdminActiveFilter, AdminPage, AdminUser } from "@/types/admin";

const ACTIVE_OPTIONS = [
  { id: "ALL" as const, label: "All" },
  { id: "ACTIVE" as const, label: "Active" },
  { id: "INACTIVE" as const, label: "Inactive" },
];

export function AdminUsersPanel() {
  const [q, setQ] = useState("");
  const [active, setActive] = useState<AdminActiveFilter>("ALL");
  const [page, setPage] = useState<AdminPage<AdminUser> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ active, page: "0", size: "25" });
    if (q.trim()) params.set("q", q.trim());
    try {
      const response = await fetch(`/api/admin/users?${params.toString()}`, { cache: "no-store" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setPage((await response.json()) as AdminPage<AdminUser>);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load users");
      setPage(null);
    } finally {
      setLoading(false);
    }
  }, [active, q]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="flex flex-wrap items-end gap-4">
          <label className="min-w-[220px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Search
            </span>
            <input
              value={q}
              onChange={(event) => setQ(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") void load();
              }}
              placeholder="Email, display name, or Clerk id"
              className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
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
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Display name</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Email</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Clerk id</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={4} className="px-3 py-6 text-muted-foreground">
                  Loading users…
                </td>
              </tr>
            ) : page && page.content.length > 0 ? (
              page.content.map((user) => (
                <tr key={user.id} className="border-b border-hairline hover:bg-muted/20">
                  <td className="px-3 py-2">
                    <Link href={`/admin/users/${user.id}`} className="font-semibold hover:text-primary">
                      {user.displayName ?? "—"}
                    </Link>
                  </td>
                  <td className="px-3 py-2">{user.email}</td>
                  <td className="px-3 py-2 font-data text-[12px]">{user.clerkId}</td>
                  <td className="px-3 py-2">
                    <span
                      className={
                        user.active
                          ? "font-semibold text-primary"
                          : "font-semibold text-muted-foreground"
                      }
                    >
                      {user.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4} className="px-3 py-6 text-muted-foreground">
                  No users match this filter.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {page ? (
        <p className="font-sans text-[12px] text-muted-foreground">
          Showing {page.content.length} of{" "}
          <span className="font-data tabular-nums">{page.totalElements}</span> users
        </p>
      ) : null}
    </div>
  );
}
