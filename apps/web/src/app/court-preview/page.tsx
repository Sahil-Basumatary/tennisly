"use client";

import dynamic from "next/dynamic";
import { useMemo, useState } from "react";
import type { Surface } from "@/types/replay";
import {
  CAMERA_PRESET_LABELS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "@/components/court/scene/cameraPresets";
import type { PlayerGender } from "@/components/court/scene/loadPlayer";
import { formatShotType } from "@/lib/shot-labels";
import { PLAYBACK_SPEEDS, usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

const CourtViz = dynamic(
  () => import("@/components/court/CourtViz").then((m) => m.CourtViz),
  {
    ssr: false,
    loading: () => (
      <div className="flex min-h-[70vh] items-center justify-center bg-[#0b5c2e] font-sans text-xs font-semibold uppercase tracking-wide text-white/80">
        Loading court…
      </div>
    ),
  },
);

const SURFACES: Surface[] = ["GRASS", "CLAY", "HARD"];
const PRESETS = Object.keys(CAMERA_PRESET_LABELS) as CameraPresetId[];
const OVERLAY_KEYS = [
  { key: "arcs" as const, label: "Arcs" },
  { key: "landings" as const, label: "Landings" },
  { key: "serveBox" as const, label: "Serve box" },
];

type TourLine = "men" | "women" | "mixed";

const TOUR_GENDERS: Record<TourLine, { home: PlayerGender; away: PlayerGender }> = {
  men: { home: "male", away: "male" },
  women: { home: "female", away: "female" },
  mixed: { home: "male", away: "female" },
};

export default function CourtPreviewPage() {
  const [surface, setSurface] = useState<Surface>("GRASS");
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const [tour, setTour] = useState<TourLine>("men");
  const genders = useMemo(() => TOUR_GENDERS[tour], [tour]);
  const playing = usePlayback((s) => s.playing);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const durationSeconds = usePlayback((s) => s.durationSeconds);
  const speed = usePlayback((s) => s.speed);
  const toggle = usePlayback((s) => s.toggle);
  const seek = usePlayback((s) => s.seek);
  const setSpeed = usePlayback((s) => s.setSpeed);
  const shots = useReplaySession((s) => s.shots);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const activeShot = shots[activeShotIndex] ?? null;

  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <header className="mb-6">
        <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
          Court viz · M5 preview
        </p>
        <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
          Broadcast court scene
        </h1>
        <p className="mt-1 max-w-2xl font-sans text-sm text-muted-foreground">
          Shot arcs, landing markers, and serve-box highlight. Toggle overlays independently.
        </p>
      </header>
      <div className="mb-3 flex flex-wrap gap-2">
        {SURFACES.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setSurface(s)}
            className={
              surface === s
                ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
            }
          >
            {s.toLowerCase()}
          </button>
        ))}
      </div>
      <div className="mb-3 flex flex-wrap gap-2" role="group" aria-label="Tour line">
        {(Object.keys(TOUR_GENDERS) as TourLine[]).map((id) => (
          <button
            key={id}
            type="button"
            onClick={() => setTour(id)}
            className={
              tour === id
                ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
            }
          >
            {id}
          </button>
        ))}
      </div>
      <div className="mb-3 flex flex-wrap gap-2" role="group" aria-label="Camera presets">
        {PRESETS.map((id) => (
          <button
            key={id}
            type="button"
            onClick={() => setCameraPreset(id)}
            className={
              cameraPreset === id
                ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
            }
          >
            {CAMERA_PRESET_LABELS[id]}
          </button>
        ))}
      </div>
      <div className="mb-3 flex flex-wrap gap-2" role="group" aria-label="Shot overlays">
        {OVERLAY_KEYS.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            onClick={() => toggleOverlay(key)}
            aria-pressed={overlays[key]}
            className={
              overlays[key]
                ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
            }
          >
            {label}
          </button>
        ))}
      </div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={toggle}
          className="border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
        >
          {playing ? "Pause" : "Play"}
        </button>
        <div className="flex flex-wrap gap-2" role="group" aria-label="Playback speed">
          {PLAYBACK_SPEEDS.map((rate) => (
            <button
              key={rate}
              type="button"
              onClick={() => setSpeed(rate)}
              className={
                speed === rate
                  ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                  : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
              }
            >
              {rate}×
            </button>
          ))}
        </div>
        <input
          type="range"
          min={0}
          max={durationSeconds || 1}
          step={0.01}
          value={timeSeconds}
          onChange={(e) => seek(Number(e.target.value))}
          className="h-1 w-48 accent-foreground"
          aria-label="Scrub rally time"
        />
        <span className="font-sans text-xs tabular-nums text-muted-foreground">
          {timeSeconds.toFixed(2)}s / {durationSeconds.toFixed(2)}s
        </span>
      </div>
      <div className="relative overflow-hidden border border-hairline">
        <CourtViz
          key={`${surface}-${genders.home}-${genders.away}`}
          surface={surface}
          cameraPreset={cameraPreset}
          homeGender={genders.home}
          awayGender={genders.away}
          className="min-h-[70vh] aspect-video"
          label={`${surface.toLowerCase()} tennis court in 3D`}
        />
        {activeShot ? (
          <aside
            className="absolute right-3 top-3 border border-hairline bg-white/95 px-3 py-2 shadow-sm backdrop-blur-sm"
            aria-live="polite"
          >
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.14em] text-muted-foreground">
              Shot {activeShot.shotIndex + 1}
            </p>
            <p className="font-display text-sm font-semibold text-foreground">
              {formatShotType(activeShot.shotType)}
            </p>
            <p className="mt-0.5 font-sans text-xs tabular-nums text-muted-foreground">
              {Math.round(activeShot.launchSpeedKmh)} km/h · {activeShot.hitter.toLowerCase()}
            </p>
          </aside>
        ) : null}
      </div>
    </main>
  );
}
