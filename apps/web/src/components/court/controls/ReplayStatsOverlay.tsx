"use client";

import { useMemo } from "react";
import { aggregateReplayStats, completedPointCount } from "@/lib/replay-stats";
import { cn } from "@/lib/utils";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

type ReplayStatsOverlayProps = {
  homePlayerId: string;
  awayPlayerId: string;
  homeLabel?: string;
  awayLabel?: string;
  className?: string;
};

const ROWS: {
  key: "pointsWon" | "gamesWon" | "servicePointsWon" | "breakPointsWon";
  label: string;
}[] = [
  { key: "pointsWon", label: "Pts won" },
  { key: "gamesWon", label: "Games" },
  { key: "servicePointsWon", label: "Svc pts" },
  { key: "breakPointsWon", label: "Breaks" },
];

export function ReplayStatsOverlay({
  homePlayerId,
  awayPlayerId,
  homeLabel = "H",
  awayLabel = "A",
  className,
}: ReplayStatsOverlayProps) {
  const points = useReplaySession((s) => s.points);
  const pointStarts = useReplaySession((s) => s.pointStartTimes);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const durationSeconds = usePlayback((s) => s.durationSeconds);

  const stats = useMemo(() => {
    const completed = completedPointCount(pointStarts, timeSeconds, durationSeconds);
    return aggregateReplayStats(points, homePlayerId, awayPlayerId, completed);
  }, [awayPlayerId, durationSeconds, homePlayerId, pointStarts, points, timeSeconds]);

  if (points.length === 0 || !homePlayerId || !awayPlayerId) return null;

  return (
    <aside
      className={cn(
        "pointer-events-none absolute left-2 top-36 min-w-[148px] bg-black/80 text-white backdrop-blur-sm sm:top-40",
        className,
      )}
      aria-label="Running match statistics"
      aria-live="polite"
    >
      <div className="flex items-baseline justify-between gap-2 border-b border-white/15 px-2.5 py-1">
        <p className="font-sans text-[9px] font-bold uppercase tracking-[0.16em] text-white/55">
          Match stats
        </p>
        <p className="font-data text-[10px] tabular-nums text-white/70">
          Pt {stats.completedCount}/{stats.totalCount}
        </p>
      </div>
      <div className="grid grid-cols-[1fr_auto_auto] gap-x-2.5 px-2.5 pb-1.5 pt-1">
        <span className="font-sans text-[8px] font-semibold uppercase tracking-wide text-white/40" />
        <span className="font-sans text-[8px] font-semibold uppercase tracking-wide text-white/50">
          {homeLabel}
        </span>
        <span className="font-sans text-[8px] font-semibold uppercase tracking-wide text-white/50">
          {awayLabel}
        </span>
        {ROWS.map((row) => (
          <div key={row.key} className="col-span-3 grid grid-cols-[1fr_auto_auto] gap-x-2.5 py-0.5">
            <span className="font-sans text-[10px] text-white/65">{row.label}</span>
            <span className="min-w-[1.25rem] text-right font-data text-[11px] font-semibold tabular-nums">
              {stats.home[row.key]}
            </span>
            <span className="min-w-[1.25rem] text-right font-data text-[11px] font-semibold tabular-nums">
              {stats.away[row.key]}
            </span>
          </div>
        ))}
      </div>
    </aside>
  );
}
