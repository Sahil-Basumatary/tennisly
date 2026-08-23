import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import type { TournamentBoard } from "@/types/scaffolds";
import { cn } from "@/lib/utils";

export function TournamentSkeleton({ board }: { board: TournamentBoard }) {
  return (
    <>
      <PageHero
        eyebrow="Tournaments"
        title={board.name}
        description={`${board.location} · ${board.surface}`}
      />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <section>
            <h2 className="mb-3 font-data text-[12px] font-bold uppercase tracking-[0.14em] text-chrome">
              Standings
            </h2>
            <div className="overflow-hidden border border-hairline bg-white">
              <div className="grid grid-cols-[40px_1fr_56px] gap-2 border-b border-hairline bg-chrome px-3 py-2 font-data text-[11px] font-semibold uppercase tracking-wide text-white/70">
                <span>#</span>
                <span>Player</span>
                <span className="text-right">Pts</span>
              </div>
              {board.standings.length === 0 ? (
                <p className="px-3 py-6 font-sans text-sm text-muted-foreground">
                  Rankings unavailable — start tennis-data-service with a BallDontLie API key.
                </p>
              ) : (
                board.standings.map((row) => (
                  <div
                    key={row.position}
                    className="grid grid-cols-[40px_1fr_56px] gap-2 border-b border-hairline px-3 py-2.5"
                  >
                    <span className="font-data text-[13px] text-muted-foreground">{row.position}</span>
                    <span className="truncate font-sans text-[14px] font-medium">{row.player}</span>
                    <span className="text-right font-data text-[13px] font-semibold">{row.points}</span>
                  </div>
                ))
              )}
            </div>
          </section>
          <section>
            <h2 className="mb-3 font-data text-[12px] font-bold uppercase tracking-[0.14em] text-chrome">
              Fixtures & results
            </h2>
            <div className="space-y-2">
              {board.fixtures.length === 0 ? (
                <p className="border border-hairline bg-white px-4 py-8 text-center font-sans text-sm text-muted-foreground">
                  No fixtures yet — start match-service with Live Tennis API ingestion enabled.
                </p>
              ) : (
                board.fixtures.map((fx) => (
                  <Link
                    key={fx.id}
                    href={fx.href}
                    className="block border border-hairline bg-white px-4 py-3 transition-colors hover:bg-surface-muted"
                  >
                    <div className="mb-2 flex items-center justify-between gap-2 font-data text-[11px] uppercase tracking-wide text-muted-foreground">
                      <span>{fx.round}</span>
                      <span className={cn(fx.status === "live" && "font-bold text-espn-live")}>
                        {fx.startLabel}
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-3 font-sans text-[14px]">
                      <span className="font-semibold">{fx.home}</span>
                      <span className="text-muted-foreground">vs</span>
                      <span className="font-semibold">{fx.away}</span>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </section>
        </div>
      </main>
    </>
  );
}
