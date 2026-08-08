"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useTransition } from "react";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import type { AnalyticsSurfaceFilter } from "@/types/analytics";

const SURFACE_OPTIONS = [
  { id: "ALL" as const, label: "All" },
  { id: "HARD" as const, label: "Hard" },
  { id: "CLAY" as const, label: "Clay" },
  { id: "GRASS" as const, label: "Grass" },
];

type AnalyticsFiltersProps = {
  playerId?: string;
  playerA?: string;
  playerB?: string;
  tournamentKey?: string;
  surface?: AnalyticsSurfaceFilter;
  showSurface?: boolean;
  showTournament?: boolean;
  showCompare?: boolean;
  actionLabel?: string;
};

export function AnalyticsFilters({
  playerId = "",
  playerA = "",
  playerB = "",
  tournamentKey = "",
  surface = "ALL",
  showSurface = true,
  showTournament = false,
  showCompare = false,
  actionLabel = "Apply",
}: AnalyticsFiltersProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [pending, startTransition] = useTransition();

  const pushParams = useCallback(
    (updates: Record<string, string>) => {
      const params = new URLSearchParams(searchParams.toString());
      for (const [key, value] of Object.entries(updates)) {
        if (value) params.set(key, value);
        else params.delete(key);
      }
      startTransition(() => {
        router.push(`?${params.toString()}`);
      });
    },
    [router, searchParams],
  );

  return (
    <form
      className="border border-hairline bg-white p-4 sm:p-5"
      onSubmit={(event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const data = new FormData(form);
        const nextSurface = String(data.get("surface") ?? "ALL");
        if (showCompare) {
          const a = String(data.get("playerA") ?? "");
          const b = String(data.get("playerB") ?? "");
          startTransition(() => {
            router.push(
              `/analytics/compare?playerA=${encodeURIComponent(a)}&playerB=${encodeURIComponent(b)}`,
            );
          });
          return;
        }
        if (showTournament) {
          const key = String(data.get("tournamentKey") ?? "");
          startTransition(() => {
            router.push(`/analytics/tournaments?tournamentKey=${encodeURIComponent(key)}`);
          });
          return;
        }
        const nextPlayerId = String(data.get("playerId") ?? "").trim();
        const onPlayerDetail = pathname.startsWith("/analytics/players/") && pathname !== "/analytics/players";
        if (onPlayerDetail && nextPlayerId) {
          const qs = nextSurface && nextSurface !== "ALL" ? `?surface=${nextSurface}` : "";
          startTransition(() => {
            router.push(`/analytics/players/${nextPlayerId}${qs}`);
          });
          return;
        }
        const params = new URLSearchParams();
        if (nextPlayerId) params.set("playerId", nextPlayerId);
        if (nextSurface && nextSurface !== "ALL") params.set("surface", nextSurface);
        startTransition(() => {
          router.push(`/analytics/players?${params.toString()}`);
        });
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
        {showCompare ? (
          <>
            <label className="block">
              <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Player A (UUID)
              </span>
              <input
                name="playerA"
                defaultValue={playerA}
                required
                className="w-full border border-hairline bg-white px-3 py-2 font-data text-sm outline-none focus:border-primary"
                placeholder="00000000-0000-0000-0000-000000000001"
              />
            </label>
            <label className="block">
              <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Player B (UUID)
              </span>
              <input
                name="playerB"
                defaultValue={playerB}
                required
                className="w-full border border-hairline bg-white px-3 py-2 font-data text-sm outline-none focus:border-primary"
                placeholder="00000000-0000-0000-0000-000000000002"
              />
            </label>
          </>
        ) : showTournament ? (
          <label className="block sm:col-span-2 lg:col-span-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Tournament key
            </span>
            <input
              name="tournamentKey"
              defaultValue={tournamentKey}
              required
              className="w-full border border-hairline bg-white px-3 py-2 font-sans text-sm outline-none focus:border-primary"
              placeholder="atp_australian_open"
            />
          </label>
        ) : (
          <label className="block sm:col-span-2 lg:col-span-1">
            <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Player ID (UUID)
            </span>
            <input
              name="playerId"
              defaultValue={playerId}
              required
              className="w-full border border-hairline bg-white px-3 py-2 font-data text-sm outline-none focus:border-primary"
              placeholder="00000000-0000-0000-0000-000000000001"
            />
          </label>
        )}
        {showSurface ? (
          <SegmentedControl
            label="Surface"
            options={SURFACE_OPTIONS}
            value={surface}
            onChange={(next) => {
              if (pathname.startsWith("/analytics/players/") && pathname !== "/analytics/players") {
                pushParams({ surface: next === "ALL" ? "" : next });
              } else {
                pushParams({ surface: next });
              }
            }}
          />
        ) : null}
        <button
          type="submit"
          disabled={pending}
          className="h-10 border border-primary bg-primary px-4 font-sans text-[12px] font-semibold uppercase tracking-wide text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-60"
        >
          {pending ? "Loading…" : actionLabel}
        </button>
      </div>
      {showSurface ? <input type="hidden" name="surface" value={surface} /> : null}
    </form>
  );
}
