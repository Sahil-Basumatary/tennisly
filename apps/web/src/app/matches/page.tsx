import Link from "next/link";
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
      <SectionSubnav items={subnav} activeId="live" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
          Matches
        </p>
        <h1 className="mb-6 font-display text-2xl font-semibold text-foreground sm:text-3xl">
          Live Centre
        </h1>
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
                  <span className="font-data text-[12px] font-bold uppercase tracking-wide text-primary">
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
