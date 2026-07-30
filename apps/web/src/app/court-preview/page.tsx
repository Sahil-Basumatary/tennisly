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
import { PLAYBACK_SPEEDS, usePlayback } from "@/stores/playback";

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

  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <header className="mb-6">
        <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
          Court viz · M4 preview
        </p>
        <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
          Broadcast court scene
        </h1>
        <p className="mt-1 max-w-2xl font-sans text-sm text-muted-foreground">
          Rally playback with athletes, speed control, and tour line models. Orbit or use camera
          presets.
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
      <div className="overflow-hidden border border-hairline">
        <CourtViz
          key={`${surface}-${genders.home}-${genders.away}`}
          surface={surface}
          cameraPreset={cameraPreset}
          homeGender={genders.home}
          awayGender={genders.away}
          className="min-h-[70vh] aspect-video"
          label={`${surface.toLowerCase()} tennis court in 3D`}
        />
      </div>
    </main>
  );
}
