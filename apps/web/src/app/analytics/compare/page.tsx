import Link from "next/link";
import { Suspense } from "react";
import { AnalyticsErrorPanel } from "@/components/analytics/AnalyticsErrorPanel";
import { AnalyticsFilters } from "@/components/analytics/AnalyticsFilters";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { H2HBarChart } from "@/components/analytics/H2HBarChart";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import {
  AnalyticsUpstreamError,
  fetchUpstreamCompare,
} from "@/lib/analytics-upstream";

type PageProps = {
  searchParams: Promise<{ playerA?: string; playerB?: string; from?: string; to?: string }>;
};

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export default async function CompareAnalyticsPage({ searchParams }: PageProps) {
  const query = await searchParams;
  const playerA = query.playerA?.trim() ?? "";
  const playerB = query.playerB?.trim() ?? "";

  let compare = null;
  let errorMessage: string | undefined;
  if (playerA && playerB) {
    try {
      compare = await fetchUpstreamCompare(playerA, playerB, {
        from: query.from,
        to: query.to,
      });
    } catch (err) {
      errorMessage =
        err instanceof AnalyticsUpstreamError && err.status === 404
          ? "No head-to-head data for these players."
          : undefined;
    }
  }

  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="compare" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title="Compare players"
          description="Head-to-head wins and cumulative tape metrics across every indexed meeting."
        />
        <div className="mb-6">
          <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading filters…</p>}>
            <AnalyticsFilters
              showCompare
              showSurface={false}
              playerA={playerA}
              playerB={playerB}
              actionLabel="Compare"
            />
          </Suspense>
        </div>
        {errorMessage !== undefined ? (
          <AnalyticsErrorPanel message={errorMessage} />
        ) : compare ? (
          <div className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-4">
              {[
                { label: "Meetings", value: compare.meetingCount },
                { label: "A wins", value: compare.aWins },
                { label: "B wins", value: compare.bWins },
                { label: "Unknown", value: compare.unknownResults },
              ].map((stat) => (
                <div key={stat.label} className="border border-hairline bg-white p-4">
                  <p className="mb-1 font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                    {stat.label}
                  </p>
                  <p className="font-data text-2xl font-semibold tabular-nums">{stat.value}</p>
                </div>
              ))}
            </div>
            <H2HBarChart
              playerA={compare.playerA}
              playerB={compare.playerB}
              labelA={playerA.slice(0, 8)}
              labelB={playerB.slice(0, 8)}
            />
            <div className="border border-hairline bg-white">
              <div className="border-b border-hairline px-4 py-3 sm:px-5">
                <h2 className="font-sans text-[13px] font-bold uppercase tracking-wide">
                  Meetings
                </h2>
              </div>
              {compare.meetings.length === 0 ? (
                <p className="px-4 py-6 font-sans text-[13px] text-muted-foreground sm:px-5">
                  No indexed meetings between these players.
                </p>
              ) : (
                <ul className="divide-y divide-hairline">
                  {compare.meetings.map((meeting) => (
                    <li key={meeting.matchId} className="px-4 py-3 sm:px-5">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <Link
                          href={`/analytics/matches/${meeting.matchId}`}
                          className="font-sans text-[14px] font-semibold hover:text-primary"
                        >
                          {meeting.tournamentName}
                        </Link>
                        <span className="font-data text-[12px] text-muted-foreground">
                          {formatDate(meeting.endedAt ?? meeting.scheduledAt)}
                        </span>
                      </div>
                      <p className="font-sans text-[12px] text-muted-foreground">
                        {meeting.surface} · A {meeting.playerAMetrics.pointsWon} pts · B{" "}
                        {meeting.playerBMetrics.pointsWon} pts
                        {meeting.playerAWon === true
                          ? " · A won"
                          : meeting.playerAWon === false
                            ? " · B won"
                            : ""}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ) : (
          <p className="font-sans text-[13px] text-muted-foreground">
            Enter two player UUIDs above to load head-to-head analytics.
          </p>
        )}
      </main>
    </>
  );
}
