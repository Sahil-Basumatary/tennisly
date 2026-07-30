"use client";

import { PLAYBACK_SPEEDS, usePlayback } from "@/stores/playback";
import { cn } from "@/lib/utils";

type TransportBarProps = {
  className?: string;
};

/**
 * Video-player transport docked over the canvas: gradient scrim, scrubber
 * with progress fill, play/pause icon, speed steps, clock readout.
 */
export function TransportBar({ className }: TransportBarProps) {
  const playing = usePlayback((s) => s.playing);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const durationSeconds = usePlayback((s) => s.durationSeconds);
  const speed = usePlayback((s) => s.speed);
  const toggle = usePlayback((s) => s.toggle);
  const seek = usePlayback((s) => s.seek);
  const setSpeed = usePlayback((s) => s.setSpeed);
  const progress = durationSeconds > 0 ? (timeSeconds / durationSeconds) * 100 : 0;

  return (
    <div className={cn("pointer-events-none absolute inset-x-0 bottom-0", className)}>
      <div className="pointer-events-auto bg-gradient-to-t from-black/85 via-black/45 to-transparent px-3 pb-2.5 pt-10 sm:px-4">
        <input
          type="range"
          min={0}
          max={durationSeconds || 1}
          step={0.01}
          value={timeSeconds}
          onChange={(e) => seek(Number(e.target.value))}
          aria-label="Scrub rally time"
          className="transport-range w-full"
          style={{
            background: `linear-gradient(to right, #fff ${progress}%, rgba(255,255,255,0.28) ${progress}%)`,
          }}
        />
        <div className="mt-2 flex items-center gap-4">
          <button
            type="button"
            onClick={toggle}
            aria-label={playing ? "Pause" : "Play"}
            className="text-white transition-opacity hover:opacity-80"
          >
            {playing ? (
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                <rect x="2.5" y="1.5" width="4" height="13" />
                <rect x="9.5" y="1.5" width="4" height="13" />
              </svg>
            ) : (
              <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                <path d="M3 1.5 14 8 3 14.5z" />
              </svg>
            )}
          </button>
          <div role="group" aria-label="Playback speed" className="flex items-center gap-3">
            {PLAYBACK_SPEEDS.map((rate) => (
              <button
                key={rate}
                type="button"
                onClick={() => setSpeed(rate)}
                aria-pressed={speed === rate}
                className={cn(
                  "font-data text-[12px] font-bold tabular-nums tracking-wide transition-colors",
                  speed === rate ? "text-white" : "text-white/50 hover:text-white/80",
                )}
              >
                {rate}×
              </button>
            ))}
          </div>
          <span className="ml-auto font-data text-[11px] tabular-nums tracking-wide text-white/85">
            {timeSeconds.toFixed(2)}s / {durationSeconds.toFixed(2)}s
          </span>
        </div>
      </div>
    </div>
  );
}
