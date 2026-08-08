"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import type { AdminOrgMember, AdminOrganization, PlanTier } from "@/types/admin";

const PLAN_OPTIONS: PlanTier[] = ["FREE", "BASIC", "PRO", "ENTERPRISE"];

type AdminOrganizationDetailProps = {
  orgId: string;
};

export function AdminOrganizationDetail({ orgId }: AdminOrganizationDetailProps) {
  const [org, setOrg] = useState<AdminOrganization | null>(null);
  const [members, setMembers] = useState<AdminOrgMember[]>([]);
  const [planTier, setPlanTier] = useState<PlanTier>("FREE");
  const [maxMembers, setMaxMembers] = useState(10);
  const [active, setActive] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [orgResponse, membersResponse] = await Promise.all([
        fetch(`/api/admin/organizations/${orgId}`, { cache: "no-store" }),
        fetch(`/api/admin/organizations/${orgId}/members`, { cache: "no-store" }),
      ]);
      if (!orgResponse.ok) {
        const body = (await orgResponse.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Organization request failed (${orgResponse.status})`);
      }
      const orgData = (await orgResponse.json()) as AdminOrganization;
      setOrg(orgData);
      setPlanTier(orgData.planTier);
      setMaxMembers(orgData.maxMembers);
      setActive(orgData.active);
      if (membersResponse.ok) {
        setMembers((await membersResponse.json()) as AdminOrgMember[]);
      } else {
        setMembers([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load organization");
      setOrg(null);
    } finally {
      setLoading(false);
    }
  }, [orgId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function save() {
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const response = await fetch(`/api/admin/organizations/${orgId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ planTier, maxMembers, active }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Save failed (${response.status})`);
      }
      const updated = (await response.json()) as AdminOrganization;
      setOrg(updated);
      setMessage("Organization updated.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save organization");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <p className="font-sans text-sm text-muted-foreground">Loading organization…</p>;
  }

  if (!org) {
    return (
      <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
        {error ?? "Organization not found."}
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <div className="mb-4 grid gap-3 sm:grid-cols-2">
          <div>
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Name
            </p>
            <p className="font-sans text-[14px] font-semibold">{org.name}</p>
          </div>
          <div>
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Clerk org id
            </p>
            <p className="font-data text-[12px]">{org.clerkOrgId}</p>
          </div>
          <div>
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Slug
            </p>
            <p className="font-data text-[12px]">{org.slug}</p>
          </div>
          <div>
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Updated
            </p>
            <p className="font-data text-[12px] tabular-nums">
              {new Date(org.updatedAt).toLocaleString()}
            </p>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Plan tier
            </span>
            <select
              value={planTier}
              onChange={(event) => setPlanTier(event.target.value as PlanTier)}
              className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
            >
              {PLAN_OPTIONS.map((tier) => (
                <option key={tier} value={tier}>
                  {tier}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Max members
            </span>
            <input
              type="number"
              min={1}
              value={maxMembers}
              onChange={(event) => setMaxMembers(Number(event.target.value))}
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label className="flex items-end gap-2 pb-2">
            <input
              id="org-active"
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
            {saving ? "Saving…" : "Save changes"}
          </button>
          <Link href="/admin/organizations" className="font-sans text-[13px] font-semibold text-primary hover:underline">
            Back to organizations
          </Link>
        </div>
        {message ? <p className="mt-3 font-sans text-sm text-primary">{message}</p> : null}
        {error ? <p className="mt-3 font-sans text-sm text-destructive">{error}</p> : null}
      </div>
      <div className="overflow-x-auto border border-hairline bg-white">
        <h2 className="border-b border-hairline px-3 py-2 font-sans text-[13px] font-bold uppercase tracking-wide">
          Members
        </h2>
        <table className="min-w-full border-collapse font-sans text-[13px]">
          <thead>
            <tr className="border-b border-hairline bg-muted/30 text-left">
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Name</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Email</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Role</th>
            </tr>
          </thead>
          <tbody>
            {members.length > 0 ? (
              members.map((member) => (
                <tr key={member.id} className="border-b border-hairline">
                  <td className="px-3 py-2">{member.displayName ?? "—"}</td>
                  <td className="px-3 py-2">{member.email}</td>
                  <td className="px-3 py-2 font-data">{member.role}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={3} className="px-3 py-6 text-muted-foreground">
                  No members loaded for this organization.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
