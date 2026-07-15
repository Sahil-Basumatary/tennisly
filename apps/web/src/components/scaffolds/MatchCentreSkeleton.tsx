import type { MatchCentrePanel } from "@/types/scaffolds";
import { cn } from "@/lib/utils";

export function MatchCentreSkeleton({ match }: { match: MatchCentrePanel }) {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
            Match centre
          </p>
          <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
            {match.home.name} vs {match.away.name}
          </h1>
          <p className="mt-1 font-sans text-sm text-muted-foreground">
            {match.tournament} · {match.round} · {match.court}
          </p>
        </div>
        <span
          className={cn(
            "font-data text-xs font-bold uppercase tracking-wide",
            match.status === "live" ? "text-[#da1e28]" : "text-muted-foreground",
          )}
        >
          {match.status}
        </span>
      </div>
      <div className="grid gap-4 lg:grid-cols-[0.9fr_1.2fr_0.9fr]">
        <aside className="border border-hairline bg-white p-4">
          <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
            Line-ups
          </h2>
          <div className="space-y-4">
            <div>
              <p className="font-sans text-[14px] font-semibold">
                {match.home.seed ? `${match.home.seed}. ` : null}
                {match.home.name}
              </p>
              <p className="font-data text-[12px] text-muted-foreground">
                {match.home.country}
              </p>
            </div>
            <div className="border-t border-hairline pt-4">
              <p className="font-sans text-[14px] font-semibold">
                {match.away.seed ? `${match.away.seed}. ` : null}
                {match.away.name}
              </p>
              <p className="font-data text-[12px] text-muted-foreground">
                {match.away.country}
              </p>
            </div>
          </div>
        </aside>
        <section className="flex min-h-[320px] flex-col border border-hairline bg-white">
          <div className="border-b border-hairline px-4 py-3">
            <div className="flex items-center justify-between gap-4 font-sans text-[15px] font-semibold">
              <span>{match.home.name}</span>
              <span className="font-data tabular-nums tracking-wide">
                {match.score.homeSets.join(" ") || "—"}
              </span>
            </div>
            <div className="mt-2 flex items-center justify-between gap-4 font-sans text-[15px] font-semibold">
              <span>{match.away.name}</span>
              <span className="font-data tabular-nums tracking-wide">
                {match.score.awaySets.join(" ") || "—"}
              </span>
            </div>
          </div>
          <div className="relative flex flex-1 items-center justify-center bg-[linear-gradient(180deg,#0b5c2e_0%,#087038_45%,#0b5c2e_100%)] p-6">
            <div
              className="h-full w-full max-w-md border-2 border-white/70"
              aria-hidden
            >
              <div className="relative h-full w-full">
                <div className="absolute inset-x-0 top-1/2 h-px -translate-y-1/2 bg-white/70" />
                <div className="absolute inset-y-[12%] left-1/2 w-px -translate-x-1/2 bg-white/50" />
                <div className="absolute left-1/2 top-1/2 h-16 w-24 -translate-x-1/2 -translate-y-1/2 border border-white/60" />
              </div>
            </div>
            <p className="absolute bottom-3 left-1/2 -translate-x-1/2 font-sans text-[11px] font-semibold uppercase tracking-wide text-white/80">
              Court viz slot
            </p>
          </div>
        </section>
        <aside className="border border-hairline bg-white p-4">
          <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
            Match stats
          </h2>
          <div className="space-y-3">
            {match.stats.map((stat) => (
              <div key={stat.label}>
                <p className="mb-1 text-center font-sans text-[11px] uppercase tracking-wide text-muted-foreground">
                  {stat.label}
                </p>
                <div className="grid grid-cols-3 items-center gap-2 font-data text-[13px]">
                  <span className="text-left font-semibold">{stat.home}</span>
                  <span className="text-center text-muted-foreground">—</span>
                  <span className="text-right font-semibold">{stat.away}</span>
                </div>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </div>
  );
}
