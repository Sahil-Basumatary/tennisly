"use client";

import { useCallback, useState } from "react";
import type { AdminCreateWebhookResponse, AdminWebhookEndpoint } from "@/types/admin";

const EVENT_OPTIONS = [
  "match.completed",
  "match.point_recorded",
  "api_key.revoked",
  "webhook.test",
] as const;

function formatInstant(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

export function AdminWebhooksPanel() {
  const [organizationId, setOrganizationId] = useState("");
  const [endpoints, setEndpoints] = useState<AdminWebhookEndpoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createOrgId, setCreateOrgId] = useState("");
  const [createName, setCreateName] = useState("");
  const [createUrl, setCreateUrl] = useState("");
  const [createEvents, setCreateEvents] = useState<string[]>(["match.completed", "webhook.test"]);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [plaintextSecret, setPlaintextSecret] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async (orgId: string) => {
    if (!orgId.trim()) {
      setError("Organization ID is required to list webhooks");
      setEndpoints([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ organizationId: orgId.trim() });
      const response = await fetch(`/api/admin/webhooks?${params.toString()}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setEndpoints((await response.json()) as AdminWebhookEndpoint[]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load webhooks");
      setEndpoints([]);
    } finally {
      setLoading(false);
    }
  }, []);

  function toggleEvent(eventType: string) {
    setCreateEvents((current) =>
      current.includes(eventType)
        ? current.filter((value) => value !== eventType)
        : [...current, eventType],
    );
  }

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    setCreating(true);
    setCreateError(null);
    setPlaintextSecret(null);
    try {
      if (createEvents.length === 0) {
        throw new Error("Select at least one event type");
      }
      const response = await fetch("/api/admin/webhooks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          organizationId: createOrgId.trim(),
          name: createName.trim(),
          targetUrl: createUrl.trim(),
          eventTypes: createEvents,
        }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Create failed (${response.status})`);
      }
      const data = (await response.json()) as AdminCreateWebhookResponse;
      setPlaintextSecret(data.plaintextSecret);
      setOrganizationId(createOrgId.trim());
      setCreateName("");
      setCreateUrl("");
      await load(createOrgId.trim());
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : "Failed to create webhook");
    } finally {
      setCreating(false);
    }
  }

  async function runAction(
    id: string,
    orgId: string,
    action: "revoke" | "rotate-secret" | "test",
  ) {
    setBusyId(`${action}:${id}`);
    setError(null);
    try {
      const params = new URLSearchParams({ organizationId: orgId });
      const response = await fetch(`/api/admin/webhooks/${id}/${action}?${params.toString()}`, {
        method: "POST",
      });
      if (!response.ok && response.status !== 202) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `${action} failed (${response.status})`);
      }
      if (action === "rotate-secret") {
        const data = (await response.json()) as AdminCreateWebhookResponse;
        setPlaintextSecret(data.plaintextSecret);
      }
      await load(orgId);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${action}`);
    } finally {
      setBusyId(null);
    }
  }

  async function copySecret() {
    if (!plaintextSecret) return;
    await navigator.clipboard.writeText(plaintextSecret);
  }

  return (
    <div className="space-y-4">
      {plaintextSecret ? (
        <div className="border border-primary bg-primary/5 p-4 sm:p-5">
          <p className="font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-primary">
            Copy this signing secret now — it will not be shown again
          </p>
          <p className="mt-2 break-all font-data text-[13px] tabular-nums">{plaintextSecret}</p>
          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void copySecret()}
              className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
            >
              Copy secret
            </button>
            <button
              type="button"
              onClick={() => setPlaintextSecret(null)}
              className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide"
            >
              Dismiss
            </button>
          </div>
        </div>
      ) : null}
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-3 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Register endpoint
        </h2>
        <form onSubmit={(event) => void handleCreate(event)} className="space-y-4">
          <div className="flex flex-wrap items-end gap-4">
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
                placeholder="Partner webhook"
                required
                maxLength={100}
                className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
              />
            </label>
          </div>
          <label className="block">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Target URL
            </span>
            <input
              value={createUrl}
              onChange={(event) => setCreateUrl(event.target.value)}
              placeholder="https://example.com/hooks/tennisly"
              required
              maxLength={2048}
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <fieldset>
            <legend className="mb-2 font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Event types
            </legend>
            <div className="flex flex-wrap gap-3">
              {EVENT_OPTIONS.map((eventType) => (
                <label key={eventType} className="flex items-center gap-2 font-data text-[12px]">
                  <input
                    type="checkbox"
                    checked={createEvents.includes(eventType)}
                    onChange={() => toggleEvent(eventType)}
                  />
                  {eventType}
                </label>
              ))}
            </div>
          </fieldset>
          <button
            type="submit"
            disabled={creating}
            className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
          >
            {creating ? "Creating…" : "Create webhook"}
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
              Organization ID
            </span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="Organization UUID"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <button
            type="button"
            onClick={() => void load(organizationId)}
            className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
          >
            Load
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
              <th className="px-3 py-2 font-bold uppercase tracking-wide">URL</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Events</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Status</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Last delivery</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-muted-foreground">
                  Loading webhooks…
                </td>
              </tr>
            ) : endpoints.length > 0 ? (
              endpoints.map((endpoint) => (
                <tr key={endpoint.id} className="border-b border-hairline hover:bg-muted/20">
                  <td className="px-3 py-2 font-semibold">
                    {endpoint.name}
                    <div className="font-data text-[11px] text-muted-foreground">
                      {endpoint.secretPrefix}…
                    </div>
                  </td>
                  <td className="max-w-[240px] truncate px-3 py-2 font-data text-[12px]">
                    {endpoint.targetUrl}
                  </td>
                  <td className="px-3 py-2 font-data text-[11px]">
                    {endpoint.eventTypes.join(", ")}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className={
                        endpoint.active
                          ? "font-semibold text-primary"
                          : "font-semibold text-muted-foreground"
                      }
                    >
                      {endpoint.active ? "Active" : "Revoked"}
                    </span>
                  </td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {formatInstant(endpoint.lastDeliveryAt)}
                  </td>
                  <td className="px-3 py-2">
                    {endpoint.active ? (
                      <div className="flex flex-col items-start gap-1">
                        <button
                          type="button"
                          disabled={busyId === `test:${endpoint.id}`}
                          onClick={() =>
                            void runAction(endpoint.id, endpoint.organizationId, "test")
                          }
                          className="font-sans text-[11px] font-semibold uppercase tracking-wide text-primary disabled:opacity-60"
                        >
                          Test
                        </button>
                        <button
                          type="button"
                          disabled={busyId === `rotate-secret:${endpoint.id}`}
                          onClick={() =>
                            void runAction(endpoint.id, endpoint.organizationId, "rotate-secret")
                          }
                          className="font-sans text-[11px] font-semibold uppercase tracking-wide disabled:opacity-60"
                        >
                          Rotate
                        </button>
                        <button
                          type="button"
                          disabled={busyId === `revoke:${endpoint.id}`}
                          onClick={() =>
                            void runAction(endpoint.id, endpoint.organizationId, "revoke")
                          }
                          className="font-sans text-[11px] font-semibold uppercase tracking-wide text-destructive disabled:opacity-60"
                        >
                          Revoke
                        </button>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-muted-foreground">
                  No webhooks for this organization yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
