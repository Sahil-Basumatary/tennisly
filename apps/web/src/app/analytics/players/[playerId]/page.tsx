import { Suspense } from "react";
import { AnalyticsErrorPanel } from "@/components/analytics/AnalyticsErrorPanel";
import { AnalyticsFilters } from "@/components/analytics/AnalyticsFilters";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { PlayerAnalyticsPanel } from "@/components/analytics/PlayerAnalyticsPanel";
import { SavedViewsPanel } from "@/components/analytics/SavedViewsPanel";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import {
  AnalyticsUpstreamError,
  fetchUpstreamPlayerAnalytics,
  fetchUpstreamPlayerTrends,
} from "@/lib/analytics-upstream";
import type { AnalyticsSurfaceFilter } from "@/types/analytics";

type PageProps = {
  params: Promise<{ playerId: string }>;
  searchParams: Promise<{ surface?: string; from?: string; to?: string }>;
};

function exportHref(playerId: string, surface?: string) {
  const params = new URLSearchParams();
  if (surface && surface !== "ALL") params.set("surface", surface);
  const qs = params.toString();
  return `/api/analytics/players/${playerId}/export${qs ? `?${qs}` : ""}`;
}

export default async function PlayerAnalyticsPage({ params, searchParams }: PageProps) {
  const { playerId } = await params;
  const query = await searchParams;
  const surface = (query.surface as AnalyticsSurfaceFilter | undefined) ?? "ALL";

  let analytics;
  let trends;
  try {
    [analytics, trends] = await Promise.all([
      fetchUpstreamPlayerAnalytics(playerId, {
        surface,
        from: query.from,
        to: query.to,
      }),
      fetchUpstreamPlayerTrends(playerId, {
        surface,
        from: query.from,
        to: query.to,
      }),
    ]);
  } catch (err) {
    const message =
      err instanceof AnalyticsUpstreamError && err.status === 404
        ? "Player not found or no indexed matches."
        : undefined;
    return (
      <>
        <SectionSubnav items={analyticsSubnav} activeId="players" />
        <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
          <AnalyticsPageHeader title="Player analytics" />
          <AnalyticsErrorPanel message={message} />
        </main>
      </>
    );
  }

  const viewConfig = { type: "player", playerId, surface };

  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="players" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title={`Player ${playerId.slice(0, 8)}…`}
          description="Tape-derived points won, service points, and break points across indexed matches."
        />
        <div className="mb-6 space-y-4">
          <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading filters…</p>}>
            <AnalyticsFilters playerId={playerId} surface={surface} actionLabel="Update" />
          </Suspense>
          <Suspense fallback={null}>
            <SavedViewsPanel config={viewConfig} />
          </Suspense>
        </div>
        <PlayerAnalyticsPanel
          analytics={analytics}
          trends={trends}
          surface={surface}
          exportHref={exportHref(playerId, surface)}
        />
      </main>
    </>
  );
}
