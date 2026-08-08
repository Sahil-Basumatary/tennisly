"use client";

import { useCallback, useState } from "react";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import type {
  AdminWebhookDelivery,
  AdminWebhookDeliveryPage,
  AdminWebhookDeliveryStatus,
} from "@/types/admin";

const STATUS_OPTIONS = [
  { id: "ALL" as const, label: "All" },
  { id: "PENDING" as const, label: "Pending" },
  { id: "FAILED" as const, label: "Failed" },
  { id: "DEAD" as const, label: "Dead" },
  { id: "SUCCESS" as const, label: "Success" },
];

function formatInstant(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

function formatPayload(payload: string | null): string {
  if (!payload) return "—";
  try {
    return JSON.stringify(JSON.parse(payload) as unknown, null, 2);
  } catch {
    return payload;
  }
}

export function AdminWebhookDeliveriesPanel() {
  const [organizationId, setOrganizationId] = useState("");
  const [endpointId, setEndpointId] = useState("");
  const [eventType, setEventType] = useState("");
  const [status, setStatus] = useState<AdminWebhookDeliveryStatus | "ALL">("ALL");
  const [page, setPage] = useState<AdminWebhookDeliveryPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [selected, setSelected] = useState<AdminWebhookDelivery | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({
      status,
      page: "0",
      size: "25",
    });
    if (organizationId.trim()) params.set("organizationId", organizationId.trim());
    if (endpointId.trim()) params.set("endpointId", endpointId.trim());
    if (eventType.trim()) params.set("eventType", eventType.trim());
    try {
      const response = await fetch(`/api/admin/webhook-deliveries?${params.toString()}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Request failed (${response.status})`);
      }
      setPage((await response.json()) as AdminWebhookDeliveryPage);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load deliveries");
      setPage(null);
    } finally {
      setLoading(false);
    }
  }, [endpointId, eventType, organizationId, status]);

  async function openDetail(id: string) {
    setDetailLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/admin/webhook-deliveries/${id}`, { cache: "no-store" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Detail failed (${response.status})`);
      }
      setSelected((await response.json()) as AdminWebhookDelivery);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load delivery detail");
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleRetry(id: string) {
    setBusyId(id);
    setError(null);
    try {
      const response = await fetch(`/api/admin/webhook-deliveries/${id}/retry`, {
        method: "POST",
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(body?.error ?? `Retry failed (${response.status})`);
      }
      await load();
      if (selected?.id === id) {
        await openDetail(id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to retry delivery");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-4">
      <div className="border border-hairline bg-white p-4 sm:p-5">
        <h2 className="mb-3 font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
          Delivery log
        </h2>
        <div className="flex flex-wrap items-end gap-4">
          <label className="min-w-[220px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Organization ID
            </span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="Optional UUID"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label className="min-w-[220px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Endpoint ID
            </span>
            <input
              value={endpointId}
              onChange={(event) => setEndpointId(event.target.value)}
              placeholder="Optional UUID"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <label className="min-w-[180px] flex-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Event type
            </span>
            <input
              value={eventType}
              onChange={(event) => setEventType(event.target.value)}
              placeholder="match.completed"
              className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
            />
          </label>
          <SegmentedControl
            label="Status"
            options={STATUS_OPTIONS}
            value={status}
            onChange={setStatus}
          />
          <button
            type="button"
            onClick={() => void load()}
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
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Event</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Status</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Attempts</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">HTTP</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Created</th>
              <th className="px-3 py-2 font-bold uppercase tracking-wide">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-muted-foreground">
                  Loading deliveries…
                </td>
              </tr>
            ) : page && page.content.length > 0 ? (
              page.content.map((delivery) => (
                <tr key={delivery.id} className="border-b border-hairline hover:bg-muted/20">
                  <td className="px-3 py-2">
                    <div className="font-semibold">{delivery.eventType}</div>
                    <div className="font-data text-[11px] text-muted-foreground">
                      {delivery.eventId}
                    </div>
                  </td>
                  <td className="px-3 py-2 font-semibold">{delivery.status}</td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {delivery.attemptCount}/{delivery.maxAttempts}
                  </td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {delivery.lastHttpStatus ?? "—"}
                  </td>
                  <td className="px-3 py-2 font-data text-[12px] tabular-nums">
                    {formatInstant(delivery.createdAt)}
                  </td>
                  <td className="px-3 py-2">
                    <div className="flex flex-col items-start gap-1">
                      <button
                        type="button"
                        disabled={detailLoading}
                        onClick={() => void openDetail(delivery.id)}
                        className="font-sans text-[11px] font-semibold uppercase tracking-wide text-primary disabled:opacity-60"
                      >
                        Payload
                      </button>
                      {delivery.status !== "SUCCESS" ? (
                        <button
                          type="button"
                          disabled={busyId === delivery.id}
                          onClick={() => void handleRetry(delivery.id)}
                          className="font-sans text-[11px] font-semibold uppercase tracking-wide disabled:opacity-60"
                        >
                          {busyId === delivery.id ? "Retrying…" : "Retry"}
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-muted-foreground">
                  No deliveries match this filter. Click Load to query.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {page ? (
        <p className="font-sans text-[12px] text-muted-foreground">
          Showing {page.content.length} of{" "}
          <span className="font-data tabular-nums">{page.totalElements}</span> deliveries
        </p>
      ) : null}
      {selected ? (
        <div className="border border-hairline bg-white p-4 sm:p-5">
          <div className="mb-3 flex items-start justify-between gap-4">
            <div>
              <h3 className="font-sans text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Delivery payload
              </h3>
              <p className="mt-1 font-data text-[12px] text-muted-foreground">{selected.id}</p>
            </div>
            <button
              type="button"
              onClick={() => setSelected(null)}
              className="border border-hairline bg-white px-3 py-1.5 font-sans text-[11px] font-semibold uppercase tracking-wide"
            >
              Close
            </button>
          </div>
          {selected.lastError ? (
            <p className="mb-3 font-sans text-sm text-destructive">{selected.lastError}</p>
          ) : null}
          <pre className="overflow-x-auto border border-hairline bg-muted/20 p-3 font-data text-[12px] leading-relaxed">
            {formatPayload(selected.payload)}
          </pre>
        </div>
      ) : null}
    </div>
  );
}
