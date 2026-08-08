import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { AnalyticsEmptyState } from "@/components/analytics/AnalyticsEmptyState";
import { AnalyticsErrorPanel } from "@/components/analytics/AnalyticsErrorPanel";
import { AnalyticsFilters } from "@/components/analytics/AnalyticsFilters";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { SurfaceBreakdownChart } from "@/components/analytics/SurfaceBreakdownChart";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import {
  AnalyticsUpstreamError,
  fetchUpstreamTournamentAnalytics,
} from "@/lib/analytics-upstream";

type PageProps = {
  searchParams: Promise<{ tournamentKey?: string }>;
};

export default async function TournamentAnalyticsPage({ searchParams }: PageProps) {
  const { tournamentKey } = await searchParams;
  const key = tournamentKey?.trim() ?? "";

  if (key && tournamentKey !== key) {
    redirect(`/analytics/tournaments?tournamentKey=${encodeURIComponent(key)}`);
  }

  let tournament = null;
  let errorMessage: string | undefined;
  if (key) {
    try {
      tournament = await fetchUpstreamTournamentAnalytics(key);
    } catch (err) {
      errorMessage =
        err instanceof AnalyticsUpstreamError && err.status === 404
          ? "Tournament not found in analytics index."
          : undefined;
    }
  }

  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="tournaments" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title="Tournament analytics"
          description="Surface mix and top tape performers for a tournament key in the analytics index."
        />
        <div className="mb-6">
          <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading filters…</p>}>
            <AnalyticsFilters
              showTournament
              showSurface={false}
              tournamentKey={key}
              actionLabel="Load"
            />
          </Suspense>
        </div>
        {errorMessage !== undefined ? (
          <AnalyticsErrorPanel message={errorMessage} />
        ) : tournament ? (
          tournament.matchCount === 0 ? (
            <AnalyticsEmptyState title="No matches indexed" message="This tournament key has no indexed matches yet." />
          ) : (
            <div className="space-y-4">
              <div className="border border-hairline bg-white p-4 sm:p-5">
                <h2 className="font-display text-xl font-semibold sm:text-2xl">
                  {tournament.tournamentName ?? tournament.tournamentKey}
                </h2>
                <p className="mt-1 font-data text-sm text-muted-foreground">
                  {tournament.season ?? "—"} · {tournament.matchCount} matches indexed
                </p>
              </div>
              <SurfaceBreakdownChart
                data={Object.fromEntries(
                  Object.entries(tournament.surfaceBreakdown).map(([k, v]) => [k, Number(v)]),
                )}
                title="Matches by surface"
                valueLabel="Matches"
              />
              <div className="border border-hairline bg-white">
                <div className="border-b border-hairline px-4 py-3 sm:px-5">
                  <h2 className="font-sans text-[13px] font-bold uppercase tracking-wide">
                    Top players by points won
                  </h2>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[480px] text-left">
                    <thead>
                      <tr className="border-b border-hairline font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                        <th className="px-4 py-2.5 sm:px-5">Player</th>
                        <th className="px-4 py-2.5">Points won</th>
                      </tr>
                    </thead>
                    <tbody>
                      {tournament.topPlayers.map((player) => (
                        <tr key={player.playerId} className="border-b border-hairline last:border-0">
                          <td className="px-4 py-3 sm:px-5">
                            <Link
                              href={`/analytics/players/${player.playerId}`}
                              className="font-sans text-[14px] font-semibold hover:text-primary"
                            >
                              {player.displayName}
                            </Link>
                            <p className="font-data text-[11px] text-muted-foreground">
                              {player.playerId.slice(0, 8)}…
                            </p>
                          </td>
                          <td className="px-4 py-3 font-data text-[13px] tabular-nums">
                            {player.pointsWon}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )
        ) : (
          <p className="font-sans text-[13px] text-muted-foreground">
            Enter a tournament key above — for example a slug from your catalogue ingest.
          </p>
        )}
      </main>
    </>
  );
}
