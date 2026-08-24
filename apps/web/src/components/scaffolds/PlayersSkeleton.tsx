import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import { PlayerName } from "@/components/player/PlayerName";
import { cn } from "@/lib/utils";
import type { PlayersBoard } from "@/types/scaffolds";

type RankJump = {
  label: string;
  href: string;
  current: boolean;
};

type PlayersSkeletonProps = {
  board: PlayersBoard;
  hideHero?: boolean;
  emptyLabel?: string;
  prevHref?: string | null;
  nextHref?: string | null;
  rankJumps?: RankJump[];
};

function formatUpdatedAt(value: string): string | null {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "UTC",
  }).format(date);
}

export function PlayersSkeleton({
  board,
  hideHero,
  emptyLabel,
  prevHref,
  nextHref,
  rankJumps = [],
}: PlayersSkeletonProps) {
  const tourLabel = board.tour === "wta" ? "WTA" : "ATP";
  const total = board.total ?? board.rows.length;
  const page = board.page ?? 1;
  const size = board.size ?? Math.max(board.rows.length, 1);
  const from = total === 0 ? 0 : (page - 1) * size + 1;
  const to = Math.min(page * size, total);
  const updatedLabel = formatUpdatedAt(board.updatedAt);
  const paged = board.total != null && board.page != null && board.size != null;
  return (
    <>
      {hideHero ? null : (
        <PageHero
          eyebrow="Players"
          title={`${tourLabel} Rankings`}
          description="Official singles rankings."
        />
      )}
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <p className="font-sans text-[14px] text-muted-foreground">
            {total === 0
              ? "No ranked players on this board."
              : paged
                ? `Showing ${from}–${to} of ${total}`
                : `${total} players`}
            {updatedLabel ? ` · Updated ${updatedLabel} UTC` : null}
          </p>
          {rankJumps.length > 0 ? (
            <nav aria-label="Rank ranges" className="flex flex-wrap gap-2">
              {rankJumps.map((jump) => (
                <Link
                  key={jump.href}
                  href={jump.href}
                  aria-current={jump.current ? "page" : undefined}
                  className={cn(
                    "border px-2.5 py-1 font-data text-[11px] font-bold uppercase tracking-wide",
                    jump.current
                      ? "border-chrome bg-chrome text-chrome-foreground"
                      : "border-hairline text-chrome hover:bg-surface-muted",
                  )}
                >
                  {jump.label}
                </Link>
              ))}
            </nav>
          ) : null}
        </div>
        <div className="border border-hairline bg-white">
          <div className="sticky z-10 grid grid-cols-[48px_1fr_72px_88px] gap-2 border-b border-hairline bg-chrome px-4 py-2.5 font-data text-[11px] font-semibold uppercase tracking-wide text-white/70 sm:grid-cols-[56px_1fr_96px_112px]" style={{ top: "var(--chrome-sticky)" }}>
            <span>Rank</span>
            <span>Player</span>
            <span>Country</span>
            <span className="text-right">Points</span>
          </div>
          {board.rows.length === 0 ? (
            <p className="px-4 py-10 text-center font-sans text-sm text-muted-foreground">
              {emptyLabel ??
                "No rankings on this board right now."}
            </p>
          ) : (
            board.rows.map((row) => (
              <Link
                key={row.id}
                href={row.href}
                className="grid grid-cols-[48px_1fr_72px_88px] gap-2 border-b border-hairline px-4 py-3 transition-colors hover:bg-surface-muted sm:grid-cols-[56px_1fr_96px_112px]"
              >
                <span
                  className={cn(
                    "font-data text-[14px] tabular-nums",
                    row.rank <= 10 ? "font-bold text-foreground" : "text-muted-foreground",
                  )}
                >
                  {row.rank}
                </span>
                <PlayerName
                  name={row.name}
                  photoUrl={row.photoUrl}
                  size="md"
                  nameClassName="text-[14px] text-foreground"
                />
                <span className="font-data text-[13px] text-muted-foreground">{row.country}</span>
                <span className="text-right font-data text-[14px] tabular-nums">
                  {row.points.toLocaleString()}
                </span>
              </Link>
            ))
          )}
        </div>
        {paged && total > size ? (
          <nav aria-label="Rankings pagination" className="mt-4 flex items-center justify-between gap-3">
            {prevHref ? (
              <Link href={prevHref} className="font-sans text-[13px] font-semibold text-chrome underline">
                Previous
              </Link>
            ) : (
              <span className="font-sans text-[13px] text-muted-foreground">Previous</span>
            )}
            <p className="font-data text-[12px] uppercase tracking-wide text-muted-foreground">
              Page {page} of {Math.max(1, Math.ceil(total / size))}
            </p>
            {nextHref ? (
              <Link href={nextHref} className="font-sans text-[13px] font-semibold text-chrome underline">
                Next
              </Link>
            ) : (
              <span className="font-sans text-[13px] text-muted-foreground">Next</span>
            )}
          </nav>
        ) : null}
      </main>
    </>
  );
}
