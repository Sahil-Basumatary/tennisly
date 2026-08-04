import Link from "next/link";
import type { PlayersBoard } from "@/types/scaffolds";

export function PlayersSkeleton({ board }: { board: PlayersBoard }) {
  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
        Players
      </p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-foreground sm:text-3xl">
        {board.tour === "wta" ? "WTA" : "ATP"} Rankings
      </h1>
      <div className="overflow-hidden border border-hairline bg-white">
        <div className="grid grid-cols-[48px_1fr_72px_88px] gap-2 border-b border-hairline bg-[#f2f4f8] px-4 py-2.5 font-data text-[11px] font-semibold uppercase tracking-wide text-muted-foreground sm:grid-cols-[56px_1fr_96px_112px]">
          <span>Rank</span>
          <span>Player</span>
          <span>Country</span>
          <span className="text-right">Points</span>
        </div>
        {board.rows.length === 0 ? (
          <p className="px-4 py-10 text-center font-sans text-sm text-muted-foreground">
            No rankings yet. Start tennis-data-service (startup sync fills the mock board).
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
              <span className="font-data text-[13px] text-muted-foreground">
                {row.country}
              </span>
              <span className="text-right font-data text-[14px] tabular-nums">
                {row.points.toLocaleString()}
              </span>
            </Link>
          ))
        )}
      </div>
    </main>
  );
}
