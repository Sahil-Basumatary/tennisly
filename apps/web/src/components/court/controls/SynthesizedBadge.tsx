import { cn } from "@/lib/utils";

type SynthesizedBadgeProps = {
  className?: string;
};

/** Honest source label — ball flight is physics-simulated from a real point ledger. */
export function SynthesizedBadge({ className }: SynthesizedBadgeProps) {
  return (
    <p
      className={cn(
        "pointer-events-none absolute right-2 top-2 z-10 bg-black/80 px-2 py-1 font-sans text-[9px] font-bold uppercase tracking-[0.16em] text-white/85 backdrop-blur-sm",
        className,
      )}
      title="Ball flight is synthesized from the point ledger and surface shot priors. Shot-level tracking is not in this feed."
    >
      Synthesized trajectory
    </p>
  );
}
