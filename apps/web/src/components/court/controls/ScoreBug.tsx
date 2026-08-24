import { PlayerName } from "@/components/player/PlayerName";
import { cn } from "@/lib/utils";

export type ScoreBugSide = {
  name: string;
  photoUrl?: string | null;
  sets: number[];
  games: number;
  points: string;
  serving?: boolean;
};

type ScoreBugProps = {
  home: ScoreBugSide;
  away: ScoreBugSide;
  status?: "live" | "upcoming" | "final";
  className?: string;
};

/**
 * Broadcast lower-third score bug — sets columns, live games, point clock,
 * and a server pip. Anchored top-left so it never fights the shot chip.
 */
export function ScoreBug({ home, away, status = "live", className }: ScoreBugProps) {
  return (
    <aside
      className={cn(
        "pointer-events-none absolute left-2 top-2 min-w-[200px] bg-black/80 text-white backdrop-blur-sm",
        className,
      )}
      aria-label="Match score"
    >
      {status === "live" ? (
        <p className="border-b border-white/15 px-2.5 py-1 font-sans text-[9px] font-bold uppercase tracking-[0.18em] text-[#ff3b30]">
          Live
        </p>
      ) : null}
      <ScoreBugRow side={home} />
      <ScoreBugRow side={away} />
    </aside>
  );
}

function ScoreBugRow({ side }: { side: ScoreBugSide }) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-x-2 border-b border-white/10 px-2.5 py-1.5 last:border-b-0">
      <div className="flex min-w-0 items-center gap-1.5">
        <span
          className={cn(
            "h-1.5 w-1.5 shrink-0 rounded-full",
            side.serving ? "bg-primary" : "bg-transparent",
          )}
          aria-hidden={!side.serving}
          title={side.serving ? "Serving" : undefined}
        />
        <PlayerName
          name={side.name}
          photoUrl={side.photoUrl}
          size="xs"
          tone="dark"
          nameClassName="text-[12px] font-semibold tracking-wide text-white"
        />
      </div>
      <span className="flex gap-1 font-data text-[11px] tabular-nums text-white/70">
        {side.sets.map((set, i) => (
          <span key={`${side.name}-set-${i}`}>{set}</span>
        ))}
      </span>
      <span className="min-w-[1.25rem] text-right font-data text-[13px] font-bold tabular-nums">
        {side.games}
      </span>
      <span className="min-w-[1.75rem] text-right font-data text-[13px] font-bold tabular-nums text-white/95">
        {side.points}
      </span>
    </div>
  );
}
