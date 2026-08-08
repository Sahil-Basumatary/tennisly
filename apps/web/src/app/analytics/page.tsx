import Link from "next/link";
import { Suspense } from "react";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { SavedViewsPanel } from "@/components/analytics/SavedViewsPanel";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";

export default function AnalyticsOverviewPage() {
  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="overview" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title="Tape-provable analytics"
          description="Every metric on this board is derived from indexed point tapes — points won, service points won, and break points won. No estimated rally stats."
        />
        <div className="mb-6 grid gap-4 lg:grid-cols-2">
          <form action="/analytics/players" className="border border-hairline bg-white p-4 sm:p-5">
            <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
              Player lookup
            </h2>
            <label className="mb-3 block">
              <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Player UUID
              </span>
              <input
                name="playerId"
                required
                className="w-full border border-hairline px-3 py-2 font-data text-sm outline-none focus:border-primary"
                placeholder="00000000-0000-0000-0000-000000000001"
              />
            </label>
            <button
              type="submit"
              className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
            >
              Open player board
            </button>
          </form>
          <form action="/analytics/tournaments" className="border border-hairline bg-white p-4 sm:p-5">
            <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
              Tournament lookup
            </h2>
            <label className="mb-3 block">
              <span className="mb-1.5 block font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                Tournament key
              </span>
              <input
                name="tournamentKey"
                required
                className="w-full border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
                placeholder="atp_australian_open"
              />
            </label>
            <button
              type="submit"
              className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground"
            >
              Open tournament board
            </button>
          </form>
        </div>
        <div className="mb-6 border border-hairline bg-white p-4 sm:p-5">
          <h2 className="mb-2 font-sans text-[13px] font-bold uppercase tracking-wide">
            What you can explore
          </h2>
          <ul className="space-y-2 font-sans text-[13px] text-muted-foreground">
            <li>
              <Link href="/analytics/players" className="font-semibold text-primary hover:underline">
                Player
              </Link>{" "}
              — career summary, surface splits, trend line, match log, CSV export.
            </li>
            <li>
              <Link href="/analytics/compare" className="font-semibold text-primary hover:underline">
                Compare
              </Link>{" "}
              — head-to-head record and tape metrics for two players.
            </li>
            <li>
              <Link href="/analytics/tournaments" className="font-semibold text-primary hover:underline">
                Tournaments
              </Link>{" "}
              — surface mix and top performers for a tournament key.
            </li>
          </ul>
        </div>
        <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading saved views…</p>}>
          <SavedViewsPanel compact />
        </Suspense>
      </main>
    </>
  );
}
