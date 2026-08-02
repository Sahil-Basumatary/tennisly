import { cn } from "@/lib/utils";

type CallStampProps = {
  call: "IN" | "OUT";
  className?: string;
};

/**
 * Hawk-Eye style bounce call. Parent remounts via `key` so the CSS pop plays
 * once per bounce decision without effect timers.
 */
export function CallStamp({ call, className }: CallStampProps) {
  return (
    <div
      className={cn(
        "call-stamp-pop pointer-events-none absolute left-1/2 top-1/2",
        className,
      )}
      aria-live="polite"
    >
      <p
        className={cn(
          "border-2 px-5 py-2 font-display text-3xl font-bold tracking-[0.2em] text-white backdrop-blur-sm sm:text-4xl",
          call === "IN" ? "border-emerald-400 bg-black/70" : "border-red-500 bg-black/75",
        )}
      >
        {call}
      </p>
    </div>
  );
}
