import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";
import { AnalyticsFilters } from "@/components/analytics/AnalyticsFilters";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import type { AnalyticsSurfaceFilter } from "@/types/analytics";

type PageProps = {
  searchParams: Promise<{ playerId?: string; surface?: string }>;
};

export default async function AnalyticsPlayersSearchPage({ searchParams }: PageProps) {
  const { playerId, surface } = await searchParams;
  if (playerId?.trim()) {
    const qs = surface ? `?surface=${encodeURIComponent(surface)}` : "";
    redirect(`/analytics/players/${playerId.trim()}${qs}`);
  }

  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="players" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title="Player analytics"
          description="Enter a player UUID to load tape-derived summary, trends, and recent matches."
        />
        <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading filters…</p>}>
          <AnalyticsFilters
            playerId=""
            surface={(surface as AnalyticsSurfaceFilter) ?? "ALL"}
            actionLabel="Search"
          />
        </Suspense>
        <p className="mt-6 font-sans text-[13px] text-muted-foreground">
          Prefer a name over a UUID?{" "}
          <Link href="/players" className="font-semibold text-primary hover:underline">
            Open rankings
          </Link>
          , then use Open analytics on the player board.{" "}
          <Link href="/analytics" className="font-semibold text-primary hover:underline">
            Return to overview
          </Link>
        </p>
      </main>
    </>
  );
}
