"use client";

import dynamic from "next/dynamic";
import type { MatchCentrePanel } from "@/types/scaffolds";
import { MatchShotStatCard } from "@/components/match/MatchShotStatCard";
import { MatchStatsRail } from "@/components/match/MatchStatsRail";
import { MatchLiveBridge } from "@/components/match/MatchLiveBridge";
import { cn } from "@/lib/utils";

const MatchCourtPanel = dynamic(
  () =>
    import("@/components/court/MatchCourtPanel").then((mod) => mod.MatchCourtPanel),
  {
    ssr: false,
    loading: () => (
      <div className="flex min-h-[240px] flex-1 items-center justify-center bg-[#0b3d2e]/10 font-sans text-sm text-muted-foreground">
        Loading court…
      </div>
    ),
  },
);

export function MatchCentreSkeleton({ match }: { match: MatchCentrePanel }) {
  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <MatchLiveBridge matchId={match.id} />
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
            Match centre
          </p>
          <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
            {match.home.name} vs {match.away.name}
          </h1>
          <p className="mt-1 font-sans text-sm text-muted-foreground">
            {match.tournament} · {match.round} · {match.court}
          </p>
        </div>
        <span
          className={cn(
            "font-data text-xs font-bold uppercase tracking-wide",
            match.status === "live" ? "text-[#da1e28]" : "text-muted-foreground",
          )}
        >
          {match.status === "live" ? "LIVE" : match.status}
        </span>
      </div>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.25fr)_minmax(0,0.9fr)]">
        <aside className="order-2 border border-hairline bg-white p-4 lg:order-1">
          <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
            Line-ups
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-baseline justify-between gap-2">
                <p className="font-sans text-[14px] font-semibold">
                  {match.home.seed ? (
                    <span className="mr-1 font-data text-muted-foreground">{match.home.seed}.</span>
                  ) : null}
                  {match.home.name}
                </p>
                <span className="font-data text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {match.home.country}
                </span>
              </div>
            </div>
            <div className="border-t border-hairline pt-4">
              <div className="flex items-baseline justify-between gap-2">
                <p className="font-sans text-[14px] font-semibold">
                  {match.away.seed ? (
                    <span className="mr-1 font-data text-muted-foreground">{match.away.seed}.</span>
                  ) : null}
                  {match.away.name}
                </p>
                <span className="font-data text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {match.away.country}
                </span>
              </div>
            </div>
          </div>
        </aside>
        <section className="order-1 flex min-h-[320px] flex-col border border-hairline bg-white lg:order-2">
          <div className="border-b border-hairline px-4 py-3">
            <div className="flex items-center justify-between gap-4 font-sans text-[15px] font-semibold">
              <span className="min-w-0 truncate">{match.home.name}</span>
              <span className="shrink-0 font-data tabular-nums tracking-wide">
                {[...match.score.homeSets, match.score.homeGames].filter((n) => n !== undefined).join(" ") || "—"}
              </span>
            </div>
            <div className="mt-2 flex items-center justify-between gap-4 font-sans text-[15px] font-semibold">
              <span className="min-w-0 truncate">{match.away.name}</span>
              <span className="shrink-0 font-data tabular-nums tracking-wide">
                {[...match.score.awaySets, match.score.awayGames].filter((n) => n !== undefined).join(" ") || "—"}
              </span>
            </div>
          </div>
          <MatchCourtPanel
            homeName={match.home.name}
            awayName={match.away.name}
            score={match.score}
            status={match.status}
            matchId={match.id}
            homePlayerId={match.home.id}
            awayPlayerId={match.away.id}
            surface="GRASS"
          />
        </section>
        <MatchStatsRail
          className="order-3"
          stats={match.stats}
          homeName={match.home.name}
          awayName={match.away.name}
        >
          <MatchShotStatCard />
        </MatchStatsRail>
      </div>
    </main>
  );
}
