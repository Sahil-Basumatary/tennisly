import Link from "next/link";
import { Suspense } from "react";
import { AnalyticsPageHeader } from "@/components/analytics/AnalyticsPageHeader";
import { SavedViewsPanel } from "@/components/analytics/SavedViewsPanel";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { analyticsSubnav } from "@/config/analytics-subnav";
import { fetchUpstreamSavedViews } from "@/lib/analytics-upstream";
import { requireAnalyticsAuth } from "@/lib/analytics-bff";

type PageProps = {
  searchParams: Promise<{ view?: string }>;
};

export default async function SavedViewsPage({ searchParams }: PageProps) {
  const session = await requireAnalyticsAuth();
  const { view: highlightId } = await searchParams;
  const views =
    session ? await fetchUpstreamSavedViews(session.token, session.userId).catch(() => []) : [];

  return (
    <>
      <SectionSubnav items={analyticsSubnav} activeId="saved" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AnalyticsPageHeader
          title="Saved views"
          description="Filter presets you saved while signed in. Favorites surface on the overview board."
        />
        <Suspense fallback={<p className="font-sans text-sm text-muted-foreground">Loading…</p>}>
          <SavedViewsPanel />
        </Suspense>
        {views.length > 0 ? (
          <div className="mt-6 border border-hairline bg-white p-4">
            <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
              Quick open
            </h2>
            <ul className="space-y-2">
              {views.map((item) => {
                const config = item.config as {
                  type?: string;
                  playerId?: string;
                  tournamentKey?: string;
                  playerA?: string;
                  playerB?: string;
                  surface?: string;
                };
                let href = "/analytics";
                if (config.type === "player" && config.playerId) {
                  const qs = config.surface ? `?surface=${config.surface}` : "";
                  href = `/analytics/players/${config.playerId}${qs}`;
                } else if (config.tournamentKey) {
                  href = `/analytics/tournaments?tournamentKey=${encodeURIComponent(config.tournamentKey)}`;
                } else if (config.playerA && config.playerB) {
                  href = `/analytics/compare?playerA=${config.playerA}&playerB=${config.playerB}`;
                }
                const active = highlightId === item.id;
                return (
                  <li key={item.id}>
                    <Link
                      href={href}
                      className={`font-sans text-[14px] font-semibold hover:text-primary ${active ? "text-primary" : ""}`}
                    >
                      {item.name}
                      {item.favorite ? " · favorited" : ""}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ) : null}
      </main>
    </>
  );
}
