import Link from "next/link";
import { AnalyticsEmptyState } from "@/components/analytics/AnalyticsEmptyState";
import { SurfaceBreakdownChart } from "@/components/analytics/SurfaceBreakdownChart";
import { TrendLineChart } from "@/components/analytics/TrendLineChart";
import type { PlayerAnalytics, PlayerTrends } from "@/types/analytics";
import { cn } from "@/lib/utils";

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function surfacePoints(bySurface: PlayerAnalytics["summary"]["bySurface"]) {
  return Object.fromEntries(
    Object.entries(bySurface).map(([surface, summary]) => [surface, summary.pointsWon]),
  );
}

type PlayerAnalyticsPanelProps = {
  analytics: PlayerAnalytics;
  trends: PlayerTrends;
  surface?: string;
  exportHref: string;
  printHref?: string;
};

export function PlayerAnalyticsPanel({
  analytics,
  trends,
  surface,
  exportHref,
  printHref,
}: PlayerAnalyticsPanelProps) {
  const { summary } = analytics;
  if (summary.matchesPlayed === 0) {
    return <AnalyticsEmptyState />;
  }
  const winRate =
    summary.matchesPlayed > 0
      ? Math.round((summary.wins / summary.matchesPlayed) * 100)
      : 0;

  return (
    <div className="analytics-print-root space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: "Matches", value: summary.matchesPlayed },
          { label: "W–L", value: `${summary.wins}–${summary.losses}` },
          { label: "Win rate", value: `${winRate}%` },
          { label: "Points won", value: summary.pointsWon },
        ].map((stat) => (
          <div key={stat.label} className="border border-hairline bg-white p-4">
            <p className="mb-1 font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              {stat.label}
            </p>
            <p className="font-data text-2xl font-semibold tabular-nums">{stat.value}</p>
          </div>
        ))}
      </div>
      <div className="analytics-print-hide flex flex-wrap gap-2">
        <a
          href={exportHref}
          className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary hover:bg-surface-muted"
        >
          Download CSV
        </a>
        {printHref ? (
          <Link
            href={printHref}
            className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:bg-surface-muted"
          >
            Print report
          </Link>
        ) : null}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <TrendLineChart points={trends.trends} title={`Points won trend for player ${analytics.playerId}`} />
        <SurfaceBreakdownChart
          data={surfacePoints(summary.bySurface)}
          title={surface && surface !== "ALL" ? `${surface} points` : "Points by surface"}
          valueLabel="Points"
        />
      </div>
      <div className="border border-hairline bg-white">
        <div className="border-b border-hairline px-4 py-3 sm:px-5">
          <h2 className="font-sans text-[13px] font-bold uppercase tracking-wide">
            Recent matches
          </h2>
          <p className="font-data text-[11px] text-muted-foreground">
            Showing {analytics.matches.length} of {analytics.totalMatches}
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left">
            <thead>
              <tr className="border-b border-hairline font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                <th className="px-4 py-2.5 sm:px-5">Date</th>
                <th className="px-4 py-2.5">Opponent</th>
                <th className="px-4 py-2.5">Surface</th>
                <th className="px-4 py-2.5">Result</th>
                <th className="px-4 py-2.5">Pts</th>
                <th className="px-4 py-2.5">Svc</th>
                <th className="px-4 py-2.5">Brk</th>
              </tr>
            </thead>
            <tbody>
              {analytics.matches.map((row) => (
                <tr key={row.matchId} className="border-b border-hairline last:border-0">
                  <td className="px-4 py-3 font-data text-[13px] tabular-nums sm:px-5">
                    {formatDate(row.endedAt ?? row.scheduledAt)}
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      href={`/analytics/matches/${row.matchId}`}
                      className="font-sans text-[14px] font-semibold hover:text-primary"
                    >
                      {row.opponentName}
                    </Link>
                    <p className="font-sans text-[11px] text-muted-foreground">{row.tournamentName}</p>
                  </td>
                  <td className="px-4 py-3 font-data text-[13px] uppercase">{row.surface}</td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        "font-data text-[12px] font-bold uppercase",
                        row.won === true && "text-primary",
                        row.won === false && "text-muted-foreground",
                      )}
                    >
                      {row.won === true ? "W" : row.won === false ? "L" : "—"}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-data text-[13px] tabular-nums">{row.metrics.pointsWon}</td>
                  <td className="px-4 py-3 font-data text-[13px] tabular-nums">
                    {row.metrics.servicePointsWon}
                  </td>
                  <td className="px-4 py-3 font-data text-[13px] tabular-nums">
                    {row.metrics.breakPointsWon}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
