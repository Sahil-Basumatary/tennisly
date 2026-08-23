import Link from "next/link";
import { notFound } from "next/navigation";
import { PageHero } from "@/components/layout/PageHero";
import { getPlayerProfile } from "@/services/scaffolds";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function PlayerPage({ params }: PageProps) {
  const { id } = await params;
  const result = await getPlayerProfile(id);
  if (result.status === "missing") notFound();
  if (result.status === "unavailable") {
    return (
      <>
        <PageHero
          eyebrow="Players"
          title="Directory unavailable"
          description="The player directory is temporarily unavailable."
        />
        <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
          <p className="font-sans text-sm text-muted-foreground">
            The player directory is temporarily unavailable. Try again shortly.
          </p>
          <p className="mt-4 font-sans text-[13px]">
            <Link href="/players" className="font-semibold text-chrome underline">
              Back to rankings
            </Link>
          </p>
        </main>
      </>
    );
  }
  const player = result.player;
  const tourLabel = player.tour === "wta" ? "WTA" : "ATP";
  return (
    <>
      <PageHero
        eyebrow="Players"
        title={player.name}
        description={`${player.country} · ${tourLabel}${player.rank != null ? ` · Rank ${player.rank}` : ""}`}
      />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="mb-8 grid gap-4 sm:grid-cols-3">
          <div className="border border-hairline bg-white p-4">
            <p className="font-data text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
              Rank
            </p>
            <p className="mt-1 font-display text-2xl font-bold">
              {player.rank ?? "—"}
            </p>
          </div>
          <div className="border border-hairline bg-white p-4">
            <p className="font-data text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
              Points
            </p>
            <p className="mt-1 font-display text-2xl font-bold">
              {player.points != null ? player.points.toLocaleString() : "—"}
            </p>
          </div>
          <div className="border border-hairline bg-white p-4">
            <p className="font-data text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
              Tape
            </p>
            <Link
              href={`/analytics/players/${player.id}`}
              className="mt-2 inline-block font-sans text-[13px] font-semibold text-chrome underline"
            >
              Open analytics
            </Link>
          </div>
        </div>
        <h2 className="mb-3 font-data text-[12px] font-bold uppercase tracking-[0.14em] text-chrome">
          Matches
        </h2>
        <ul className="divide-y divide-hairline border border-hairline bg-white">
          {player.matches.length === 0 ? (
            <li className="px-4 py-8 text-center font-sans text-sm text-muted-foreground">
              No ingested matches for this player yet.
            </li>
          ) : (
            player.matches.map((match) => (
              <li key={match.id}>
                <Link
                  href={match.href}
                  className="flex items-center justify-between gap-4 px-4 py-3 hover:bg-surface-muted"
                >
                  <div>
                    <p className="font-sans text-[14px] font-semibold">
                      {match.home.name} vs {match.away.name}
                    </p>
                    <p className="font-sans text-[12px] text-muted-foreground">
                      {match.tournament} · {match.round}
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
        <p className="mt-4 font-sans text-[13px] text-muted-foreground">
          <Link href={`/players?tour=${player.tour}`} className="font-semibold text-chrome underline">
            Back to {tourLabel} rankings
          </Link>
        </p>
      </main>
    </>
  );
}
