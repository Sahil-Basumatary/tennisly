import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import type { TournamentBoard } from "@/types/scaffolds";
import { PlayerName } from "@/components/player/PlayerName";
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
              {board.standingsLabel ?? "Rankings"}
            </h2>
            <div className="overflow-hidden border border-hairline bg-white">
              <div className="grid grid-cols-[40px_1fr_56px] gap-2 border-b border-hairline bg-chrome px-3 py-2 font-data text-[11px] font-semibold uppercase tracking-wide text-white/70">
                <span>#</span>
                <span>Player</span>
                <span className="text-right">Pts</span>
              </div>
              {board.standings.length === 0 ? (
                <p className="px-3 py-6 font-sans text-sm text-muted-foreground">
                  Rankings are not available right now.
                </p>
              ) : (
                board.standings.map((row) => (
                  <div
                    key={row.position}
                    className="grid grid-cols-[40px_1fr_56px] gap-2 border-b border-hairline px-3 py-2.5"
                  >
                    <span className="font-data text-[13px] text-muted-foreground">{row.position}</span>
                    <PlayerName name={row.player} photoUrl={row.photoUrl} size="sm" />
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
                  No {board.name} fixtures on the current board.
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
                      <span className={cn(fx.status === "live" && "font-bold text-live")}>
                        {fx.startLabel}
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-3 font-sans text-[14px]">
                      <PlayerName name={fx.home} photoUrl={fx.homePhotoUrl} size="sm" />
                      <span className="text-muted-foreground">vs</span>
                      <PlayerName name={fx.away} photoUrl={fx.awayPhotoUrl} size="sm" />
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
