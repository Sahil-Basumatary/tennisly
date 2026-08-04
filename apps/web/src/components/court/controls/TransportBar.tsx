"use client";

import { PLAYBACK_SPEEDS, usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";
import { indexAtOrBefore, stepPoint, stepShot } from "@/lib/replay-transport";
import { cn } from "@/lib/utils";

type TransportBarProps = {
  className?: string;
};

/**
 * Video-player transport docked over the canvas: scrubber, play/pause, speed,
 * and shot/point seeking so operators can jump like Hawk-Eye.
 */
export function TransportBar({ className }: TransportBarProps) {
  const playing = usePlayback((s) => s.playing);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const durationSeconds = usePlayback((s) => s.durationSeconds);
  const speed = usePlayback((s) => s.speed);
  const toggle = usePlayback((s) => s.toggle);
  const seek = usePlayback((s) => s.seek);
  const setSpeed = usePlayback((s) => s.setSpeed);
  const shots = useReplaySession((s) => s.shots);
  const points = useReplaySession((s) => s.points);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const shotStarts = useReplaySession((s) => s.shotStartTimes);
  const pointStarts = useReplaySession((s) => s.pointStartTimes);
  const progress = durationSeconds > 0 ? (timeSeconds / durationSeconds) * 100 : 0;
  const pointIndex = pointStarts.length > 0 ? indexAtOrBefore(pointStarts, timeSeconds) : 0;

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
        <div className="mt-2 flex items-center gap-3 sm:gap-4">
          <div className="flex items-center gap-1.5 sm:gap-2">
            <button
              type="button"
              onClick={() => stepPoint(-1)}
              disabled={pointIndex <= 0 || pointStarts.length === 0}
              aria-label="Previous point"
              className="text-white transition-opacity hover:opacity-80 disabled:opacity-30"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor" aria-hidden>
                <path d="M12 2 7 7l5 5V2zM7 2 2 7l5 5V2zM1 2h1.4v10H1V2z" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => stepShot(-1)}
              disabled={activeShotIndex <= 0 || shotStarts.length === 0}
              aria-label="Previous shot"
              className="text-white transition-opacity hover:opacity-80 disabled:opacity-30"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor" aria-hidden>
                <path d="M11 2 4.5 7 11 12V2zM3 2h1.5v10H3V2z" />
              </svg>
            </button>
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
            <button
              type="button"
              onClick={() => stepShot(1)}
              disabled={activeShotIndex >= shotStarts.length - 1 || shotStarts.length === 0}
              aria-label="Next shot"
              className="text-white transition-opacity hover:opacity-80 disabled:opacity-30"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor" aria-hidden>
                <path d="M3 2v10l6.5-5L3 2zm7.5 0H12v10h-1.5V2z" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => stepPoint(1)}
              disabled={pointIndex >= pointStarts.length - 1 || pointStarts.length === 0}
              aria-label="Next point"
              className="text-white transition-opacity hover:opacity-80 disabled:opacity-30"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor" aria-hidden>
                <path d="M2 2v10l5-5-5-5zm5 0v10l5-5-5-5zM11.6 2H13v10h-1.4V2z" />
              </svg>
            </button>
          </div>
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
            {points.length > 0 ? `Pt ${pointIndex + 1}/${points.length} · ` : ""}
            {shots.length > 0 ? `Shot ${activeShotIndex + 1}/${shots.length} · ` : ""}
            {timeSeconds.toFixed(2)}s / {durationSeconds.toFixed(2)}s
          </span>
        </div>
      </div>
    </div>
  );
}
