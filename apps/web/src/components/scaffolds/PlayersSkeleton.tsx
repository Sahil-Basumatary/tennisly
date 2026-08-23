import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import type { PlayersBoard } from "@/types/scaffolds";

type PlayersSkeletonProps = {
  board: PlayersBoard;
  hideHero?: boolean;
  emptyLabel?: string;
};

export function PlayersSkeleton({ board, hideHero, emptyLabel }: PlayersSkeletonProps) {
  const tourLabel = board.tour === "wta" ? "WTA" : "ATP";
  return (
    <>
      {hideHero ? null : (
        <PageHero
          eyebrow="Players"
          title={`${tourLabel} Rankings`}
          description="Official singles order of play, updated from tennis-data-service."
        />
      )}
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="overflow-hidden border border-hairline bg-white">
          <div className="grid grid-cols-[48px_1fr_72px_88px] gap-2 border-b border-hairline bg-chrome px-4 py-2.5 font-data text-[11px] font-semibold uppercase tracking-wide text-white/70 sm:grid-cols-[56px_1fr_96px_112px]">
            <span>Rank</span>
            <span>Player</span>
            <span>Country</span>
            <span className="text-right">Points</span>
          </div>
          {board.rows.length === 0 ? (
            <p className="px-4 py-10 text-center font-sans text-sm text-muted-foreground">
              {emptyLabel ??
                "No rankings yet. Start tennis-data-service with a BallDontLie API key so rankings can sync."}
            </p>
          ) : (
            board.rows.map((row) => (
              <Link
                key={row.id}
                href={row.href}
                className="grid grid-cols-[48px_1fr_72px_88px] gap-2 border-b border-hairline px-4 py-3 transition-colors hover:bg-surface-muted sm:grid-cols-[56px_1fr_96px_112px]"
              >
                <span className="font-data text-[14px] tabular-nums text-muted-foreground">
                  {row.rank}
                </span>
                <span className="truncate font-sans text-[14px] font-semibold text-foreground">
                  {row.name}
                </span>
                <span className="font-data text-[13px] text-muted-foreground">{row.country}</span>
                <span className="text-right font-data text-[14px] tabular-nums">
                  {row.points.toLocaleString()}
                </span>
              </Link>
            ))
          )}
        </div>
      </main>
    </>
  );
}
