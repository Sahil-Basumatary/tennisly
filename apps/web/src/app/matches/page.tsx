import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { getScoresFeed } from "@/services/scores";

const subnav = [
  { id: "live", label: "Live Centre", href: "/matches" },
  { id: "replays", label: "Replays", href: "/matches?view=replays" },
];

export default async function MatchesIndexPage() {
  const feed = await getScoresFeed();
  return (
    <>
      <PageHero
        eyebrow="Matches"
        title="Live Centre"
        description="Open any match for the scorebug, tape, and court replay."
      />
      <SectionSubnav items={subnav} activeId="live" />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <ul className="divide-y divide-hairline border border-hairline bg-white">
          {feed.items.length === 0 ? (
            <li className="px-4 py-10 text-center font-sans text-sm text-muted-foreground">
              No live-centre matches yet. Ensure match-service is ingesting from Live Tennis API.
            </li>
          ) : (
            feed.items.map((match) => (
              <li key={match.id}>
                <Link
                  href={match.href}
                  className="flex items-center justify-between gap-4 px-4 py-3 transition-colors hover:bg-surface-muted"
                >
                  <div>
                    <p className="font-sans text-[14px] font-semibold">
                      {match.home.name} vs {match.away.name}
                    </p>
                    <p className="font-sans text-[12px] text-muted-foreground">
                      {match.tournament} · {match.round}
                      {match.status === "live" ? " · LIVE" : ""}
                    </p>
                  </div>
                  <span className="font-data text-[12px] font-bold uppercase tracking-wide text-chrome">
                    Open
                  </span>
                </Link>
              </li>
            ))
          )}
        </ul>
      </main>
    </>
  );
}
