"use client";

import { useEffect, useState } from "react";
import type { AdminHealthResponse } from "@/types/admin";

export function AdminHealthPanel() {
  const [health, setHealth] = useState<AdminHealthResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const response = await fetch("/api/admin/health", { cache: "no-store" });
        if (!response.ok) throw new Error("health request failed");
        if (!cancelled) setHealth((await response.json()) as AdminHealthResponse);
      } catch {
        if (!cancelled) setHealth(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return <p className="font-sans text-sm text-muted-foreground">Checking services…</p>;
  }

  if (!health) {
    return (
      <p className="border border-hairline bg-white p-4 font-sans text-sm text-destructive">
        Could not load health snapshot.
      </p>
    );
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {health.services.map((service) => (
        <div key={service.name} className="border border-hairline bg-white p-4">
          <p className="mb-1 font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            {service.name}
          </p>
          <p
            className={
              service.status === "UP"
                ? "font-data text-2xl tabular-nums text-primary"
                : "font-data text-2xl tabular-nums text-muted-foreground"
            }
          >
            {service.status}
          </p>
          <p className="mt-1 font-sans text-[12px] text-muted-foreground">
            HTTP {service.httpStatus ?? "—"}
          </p>
        </div>
      ))}
    </div>
  );
}
