import type { ReactNode } from "react";
import { homeShare } from "@/lib/stat-share";
import { cn } from "@/lib/utils";

export type MatchStatRow = {
  label: string;
  home: string;
  away: string;
};

type MatchStatsRailProps = {
  stats: MatchStatRow[];
  homeName: string;
  awayName: string;
  className?: string;
  children?: ReactNode;
};

/**
 * UEFA-card structure + ESPN density: label, dual comparison bar, side values.
 */
export function MatchStatsRail({
  stats,
  homeName,
  awayName,
  className,
  children,
}: MatchStatsRailProps) {
  return (
    <aside className={cn("border border-hairline bg-white p-4", className)}>
      <h2 className="mb-1 font-sans text-[13px] font-bold uppercase tracking-wide">
        Match stats
      </h2>
      <div className="mb-4 flex items-center justify-between gap-2 font-data text-[11px] uppercase tracking-wide text-muted-foreground">
        <span className="truncate" title={homeName}>
          {homeName}
        </span>
        <span className="truncate text-right" title={awayName}>
          {awayName}
        </span>
      </div>
      <div className="space-y-4">
        {stats.map((stat) => {
          const share = homeShare(stat.home, stat.away);
          return (
            <div key={stat.label}>
              <p className="mb-1.5 text-center font-sans text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                {stat.label}
              </p>
              <div className="mb-1.5 flex h-1.5 overflow-hidden bg-muted">
                <div
                  className="h-full bg-foreground transition-[width] duration-300 ease-out"
                  style={{ width: `${share * 100}%` }}
                  aria-hidden
                />
                <div
                  className="h-full bg-primary/70 transition-[width] duration-300 ease-out"
                  style={{ width: `${(1 - share) * 100}%` }}
                  aria-hidden
                />
              </div>
              <div className="grid grid-cols-3 items-center gap-2 font-data text-[13px]">
                <span className="text-left font-semibold tabular-nums">{stat.home}</span>
                <span className="sr-only">
                  {stat.label}: {homeName} {stat.home}, {awayName} {stat.away}
                </span>
                <span className="text-center text-[10px] uppercase tracking-wide text-muted-foreground" aria-hidden>
                  vs
                </span>
                <span className="text-right font-semibold tabular-nums">{stat.away}</span>
              </div>
            </div>
          );
        })}
      </div>
      {children}
    </aside>
  );
}
