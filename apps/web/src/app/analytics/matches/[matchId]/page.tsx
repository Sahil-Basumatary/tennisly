import { AnalyticsErrorPanel } from "@/components/analytics/AnalyticsErrorPanel";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { MatchAnalyticsPanel } from "@/components/analytics/MatchAnalyticsPanel";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import {
  AnalyticsUpstreamError,
  fetchUpstreamMatchAnalytics,
} from "@/lib/analytics-upstream";
import type { MatchAnalytics } from "@/types/analytics";

type PageProps = {
  params: Promise<{ matchId: string }>;
  searchParams: Promise<{ print?: string }>;
};

export default async function MatchAnalyticsPage({ params, searchParams }: PageProps) {
  const { matchId } = await params;
  const { print } = await searchParams;
  const shouldPrint = print === "1";

  let match: MatchAnalytics | null = null;
  let errorMessage: string | undefined;

  try {
    match = await fetchUpstreamMatchAnalytics(matchId);
  } catch (err) {
    errorMessage =
      err instanceof AnalyticsUpstreamError && err.status === 404
        ? "Match not found in analytics index."
        : undefined;
  }

  if (!match) {
    return (
      <>
        <SectionSubnav items={analyticsSubnav} activeId="overview" />
        <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
          <AnalyticsPageHeader title="Match analytics" />
          <AnalyticsErrorPanel message={errorMessage} />
        </main>
      </>
    );
  }

  return (
    <>
      {!shouldPrint ? <SectionSubnav items={analyticsSubnav} activeId="overview" /> : null}
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        {!shouldPrint ? (
          <AnalyticsPageHeader
            title="Match analytics"
            description="Point-tape metrics for this indexed match."
          />
        ) : null}
        <MatchAnalyticsPanel match={match} print={shouldPrint} />
      </main>
    </>
  );
}
