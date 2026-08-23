import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { getScoresFeed } from "@/services/scores";

const subnav = [
  { id: "live", label: "Live Centre", href: "/matches?status=live" },
  { id: "replays", label: "Replays", href: "/matches?view=replays" },
];

type PageProps = {
  searchParams: Promise<{ view?: string; status?: string }>;
};

export default async function MatchesIndexPage({ searchParams }: PageProps) {
  const { view, status } = await searchParams;
  const replays = view === "replays";
  const uiStatus = replays ? "final" : status === "live" ? "live" : undefined;
  const feed = await getScoresFeed(uiStatus);
  const activeId = replays ? "replays" : "live";
  return (
    <>
      <PageHero
        eyebrow="Matches"
        title={replays ? "Replays" : "Live Centre"}
        description={
          replays
            ? "Completed matches with tape and court replay."
            : "Open any match for the scorebug, tape, and court replay."
        }
      />
      <SectionSubnav items={subnav} activeId={activeId} />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <ul className="divide-y divide-hairline border border-hairline bg-white">
          {feed.items.length === 0 ? (
            <li className="px-4 py-10 text-center font-sans text-sm text-muted-foreground">
              {replays
                ? "No completed matches on the board right now."
                : status === "live"
                  ? "No live matches right now. Check replays for completed tapes."
                  : "No live-centre matches on the board right now."}
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
