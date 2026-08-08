import Link from "next/link";
import { MatchStatsRail } from "@/components/match/MatchStatsRail";
import { PrintOnLoad } from "@/components/analytics/PrintOnLoad";
import type { MatchAnalytics } from "@/types/analytics";

type MatchAnalyticsPanelProps = {
  match: MatchAnalytics;
  print?: boolean;
};

export function MatchAnalyticsPanel({ match, print = false }: MatchAnalyticsPanelProps) {
  const stats = [
    {
      label: "Points won",
      home: String(match.homeMetrics.pointsWon),
      away: String(match.awayMetrics.pointsWon),
    },
    {
      label: "Service points",
      home: String(match.homeMetrics.servicePointsWon),
      away: String(match.awayMetrics.servicePointsWon),
    },
    {
      label: "Break points",
      home: String(match.homeMetrics.breakPointsWon),
      away: String(match.awayMetrics.breakPointsWon),
    },
  ];

  return (
    <div className="analytics-print-root space-y-4">
      {print ? <PrintOnLoad enabled /> : null}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.85fr)]">
        <div className="border border-hairline bg-white p-4 sm:p-5">
          <p className="mb-1 font-data text-[11px] uppercase tracking-wide text-muted-foreground">
            {match.tournamentName ?? match.tournamentKey ?? "Tournament"} · {match.surface ?? "—"}
          </p>
          <h2 className="mb-4 font-display text-xl font-semibold sm:text-2xl">
            {match.homeDisplayName} vs {match.awayDisplayName}
          </h2>
          <dl className="grid gap-3 sm:grid-cols-2">
            <div>
              <dt className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Status
              </dt>
              <dd className="font-data text-sm uppercase">{match.status ?? "—"}</dd>
            </div>
            <div>
              <dt className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Points played
              </dt>
              <dd className="font-data text-sm tabular-nums">{match.pointsPlayed}</dd>
            </div>
            <div>
              <dt className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Scheduled
              </dt>
              <dd className="font-data text-sm">
                {match.scheduledAt ? new Date(match.scheduledAt).toLocaleString() : "—"}
              </dd>
            </div>
            <div>
              <dt className="font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Ended
              </dt>
              <dd className="font-data text-sm">
                {match.endedAt ? new Date(match.endedAt).toLocaleString() : "—"}
              </dd>
            </div>
          </dl>
          <div className="analytics-print-hide mt-4 flex flex-wrap gap-2">
            <a
              href={`/api/analytics/matches/${match.matchId}/export`}
              className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary hover:bg-surface-muted"
            >
              Download CSV
            </a>
            <Link
              href={`/analytics/matches/${match.matchId}?print=1`}
              className="border border-hairline bg-white px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:bg-surface-muted"
            >
              Print
            </Link>
          </div>
        </div>
        <MatchStatsRail
          stats={stats}
          homeName={match.homeDisplayName}
          awayName={match.awayDisplayName}
        />
      </div>
    </div>
  );
}
