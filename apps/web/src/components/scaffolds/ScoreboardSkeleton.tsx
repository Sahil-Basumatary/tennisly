import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";
import type { ScoreboardDay } from "@/types/scaffolds";
import type { ScoreCard } from "@/types/scores";
import { PlayerName } from "@/components/player/PlayerName";
import { cn } from "@/lib/utils";

function statusTone(status: ScoreCard["status"]) {
  if (status === "live") return "text-live";
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
          <PlayerName
            name={match.home.name}
            photoUrl={match.home.photoUrl}
            size="sm"
            bold={match.home.winner}
            nameClassName="text-[14px]"
          />
          <p className="font-data text-[14px] tabular-nums tracking-wide">
            {match.home.sets.join(" ") || "—"}
          </p>
        </div>
        <div className="flex items-center justify-between gap-3">
          <PlayerName
            name={match.away.name}
            photoUrl={match.away.photoUrl}
            size="sm"
            bold={match.away.winner}
            nameClassName="text-[14px]"
          />
          <p className="font-data text-[14px] tabular-nums tracking-wide">
            {match.away.sets.join(" ") || "—"}
          </p>
        </div>
      </div>
      <div className="hidden items-center font-sans text-[12px] font-semibold text-chrome sm:flex">
        Summary
      </div>
    </Link>
  );
}

export function ScoreboardSkeleton({ day }: { day: ScoreboardDay }) {
  return (
    <>
      <PageHero eyebrow="Scores" title="Tennis Scores" description={day.dateLabel} />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="space-y-6">
          {day.groups.length === 0 ? (
            <div className="border border-hairline bg-white px-4 py-10 text-center">
              <p className="font-sans text-sm font-semibold text-foreground">No matches on the board</p>
              <p className="mt-1 font-sans text-sm text-muted-foreground">
                No matches on the board right now.
              </p>
            </div>
          ) : (
            day.groups.map((group) => (
              <section key={group.tournamentId} className="border border-hairline bg-white">
                <header className="border-b border-hairline bg-chrome px-4 py-3 sm:px-5">
                  <h2 className="font-sans text-[15px] font-bold uppercase tracking-wide text-white">
                    {group.tournamentName}
                  </h2>
                  <p className="font-sans text-[12px] text-white/65">{group.location}</p>
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
    </>
  );
}
