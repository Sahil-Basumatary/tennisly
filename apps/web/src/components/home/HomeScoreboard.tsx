import Link from "next/link";
import type { ScoreboardDay } from "@/types/scaffolds";
import { cn } from "@/lib/utils";

export function HomeScoreboard({ day }: { day: ScoreboardDay }) {
  const groups = day.groups.slice(0, 4);
  return (
    <section className="bg-[#f4f1ea]">
      <div className="mx-auto max-w-[1400px] px-4 py-10 sm:px-6 sm:py-12">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="mb-1 font-data text-[11px] font-bold uppercase tracking-[0.16em] text-espn-live">
              Scores
            </p>
            <h2 className="font-display text-[28px] font-bold uppercase tracking-tight text-foreground">
              {day.dateLabel}
            </h2>
          </div>
          <Link
            href="/scores"
            className="font-sans text-[13px] font-semibold text-chrome hover:underline"
          >
            Full scoreboard →
          </Link>
        </div>
        {groups.length === 0 ? (
          <p className="border border-hairline bg-white px-4 py-8 font-sans text-sm text-muted-foreground">
            No matches on the board yet. Open Scores once match-service is ingesting.
          </p>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            {groups.map((group) => (
              <section key={group.tournamentId} className="border border-hairline bg-white">
                <header className="border-b border-hairline bg-chrome px-4 py-2.5">
                  <h3 className="font-sans text-[13px] font-bold uppercase tracking-wide text-white">
                    {group.tournamentName}
                  </h3>
                  <p className="font-sans text-[11px] text-white/65">{group.location}</p>
                </header>
                <ul>
                  {group.matches.slice(0, 4).map((match) => (
                    <li key={match.id} className="border-b border-hairline last:border-b-0">
                      <Link
                        href={match.href}
                        className="flex items-center justify-between gap-3 px-4 py-2.5 hover:bg-surface-muted"
                      >
                        <span className="min-w-0">
                          <span
                            className={cn(
                              "mr-2 font-data text-[10px] font-bold uppercase",
                              match.status === "live" ? "text-espn-live" : "text-muted-foreground",
                            )}
                          >
                            {match.status === "live" ? "LIVE" : match.status === "final" ? "F" : match.startLabel}
                          </span>
                          <span className="font-sans text-[13px] font-semibold">
                            {match.home.name} vs {match.away.name}
                          </span>
                        </span>
                        <span className="shrink-0 font-data text-[12px] tabular-nums">
                          {match.home.sets.join(" ") || "—"}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
