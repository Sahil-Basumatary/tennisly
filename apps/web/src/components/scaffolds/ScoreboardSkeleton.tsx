import Link from "next/link";
import type { ScoreboardDay } from "@/types/scaffolds";
import type { ScoreCard } from "@/types/scores";
import { cn } from "@/lib/utils";

function statusTone(status: ScoreCard["status"]) {
  if (status === "live") return "text-[#da1e28]";
  if (status === "final") return "text-muted-foreground";
  return "text-foreground";
}

function MatchRow({ match }: { match: ScoreCard }) {
  return (
    <Link
      href={match.href}
      className="grid grid-cols-[1fr_auto] gap-4 border-b border-hairline px-4 py-3 transition-colors hover:bg-surface-muted sm:px-5"
    >
      <div className="min-w-0 space-y-1.5">
        <div className="flex items-center gap-2 font-data text-[11px] uppercase tracking-wide text-muted-foreground">
          <span className={cn("font-bold", statusTone(match.status))}>
            {match.status === "live"
              ? "LIVE"
              : match.status === "final"
                ? "FINAL"
                : match.startLabel}
          </span>
          <span>{match.round}</span>
        </div>
        <div className="flex items-center justify-between gap-3">
          <p
            className={cn(
              "truncate font-sans text-[14px]",
              match.home.winner && "font-bold",
            )}
          >
            {match.home.name}
          </p>
          <p className="font-data text-[14px] tabular-nums tracking-wide">
            {match.home.sets.join(" ") || "—"}
          </p>
        </div>
        <div className="flex items-center justify-between gap-3">
          <p
            className={cn(
              "truncate font-sans text-[14px]",
              match.away.winner && "font-bold",
            )}
          >
            {match.away.name}
          </p>
          <p className="font-data text-[14px] tabular-nums tracking-wide">
            {match.away.sets.join(" ") || "—"}
          </p>
        </div>
      </div>
      <div className="hidden items-center font-sans text-[12px] font-semibold text-primary sm:flex">
        Summary
      </div>
    </Link>
  );
}

export function ScoreboardSkeleton({ day }: { day: ScoreboardDay }) {
  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
            Scores
          </p>
          <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
            Tennis Scores
          </h1>
        </div>
        <p className="font-data text-sm text-muted-foreground">{day.dateLabel}</p>
      </div>
      <div className="mb-6 flex gap-1 overflow-x-auto border-b border-hairline">
        {day.filters.map((filter, index) => (
          <button
            key={filter.id}
            type="button"
            className={cn(
              "shrink-0 border-b-2 px-3 py-2.5 font-sans text-[13px] font-semibold",
              index === 0
                ? "border-primary text-foreground"
                : "border-transparent text-muted-foreground",
            )}
          >
            {filter.label}
          </button>
        ))}
      </div>
      <div className="space-y-6">
        {day.groups.length === 0 ? (
          <div className="border border-hairline bg-white px-4 py-10 text-center">
            <p className="font-sans text-sm font-semibold text-foreground">No matches on the board</p>
            <p className="mt-1 font-sans text-sm text-muted-foreground">
              Start match-service with the broadcast catalogue seed, then refresh.
            </p>
          </div>
        ) : (
          day.groups.map((group) => (
            <section key={group.tournamentId} className="border border-hairline bg-white">
              <header className="border-b border-hairline bg-[#f2f4f8] px-4 py-3 sm:px-5">
                <h2 className="font-sans text-[15px] font-bold text-foreground">
                  {group.tournamentName}
                </h2>
                <p className="font-sans text-[12px] text-muted-foreground">
                  {group.location}
                </p>
              </header>
              <div>
                {group.matches.map((match) => (
                  <MatchRow key={match.id} match={match} />
                ))}
              </div>
            </section>
          ))
        )}
      </div>
    </main>
  );
}
