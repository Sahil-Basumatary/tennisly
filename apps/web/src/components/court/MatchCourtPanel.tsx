"use client";

import dynamic from "next/dynamic";
import { useEffect, useMemo, useState } from "react";
import type { Surface } from "@/types/replay";
import {
  CAMERA_PRESET_LABELS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "@/components/court/scene/cameraPresets";
import { CourtTopDownFallback } from "@/components/court/CourtTopDownFallback";
import { formatShotType } from "@/lib/shot-labels";
import { isWebGLAvailable, prefersReducedMotion } from "@/lib/webgl";
import { cn } from "@/lib/utils";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

const CourtViz = dynamic(
  () => import("@/components/court/CourtViz").then((m) => m.CourtViz),
  {
    ssr: false,
    loading: () => (
      <div className="flex min-h-[320px] items-center justify-center bg-[#0b5c2e] font-sans text-xs font-semibold uppercase tracking-wide text-white/80">
        Loading court…
      </div>
    ),
  },
);

const PRESETS = Object.keys(CAMERA_PRESET_LABELS) as CameraPresetId[];
const OVERLAY_KEYS = [
  { key: "arcs" as const, label: "Arcs" },
  { key: "landings" as const, label: "Marks" },
  { key: "heatmapHome" as const, label: "Home" },
  { key: "heatmapAway" as const, label: "Away" },
];

type MatchCourtPanelProps = {
  homeName: string;
  awayName: string;
  surface?: Surface;
  className?: string;
};

/**
 * Match-centre court slot: Babylon when WebGL works, static SVG otherwise.
 */
export function MatchCourtPanel({
  homeName,
  awayName,
  surface = "GRASS",
  className,
}: MatchCourtPanelProps) {
  const [webgl, setWebgl] = useState<boolean | null>(null);
  const [forceFallback, setForceFallback] = useState(false);
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const [reducedMotion, setReducedMotion] = useState(false);
  const playing = usePlayback((s) => s.playing);
  const toggle = usePlayback((s) => s.toggle);
  const shots = useReplaySession((s) => s.shots);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const setOverlay = useReplaySession((s) => s.setOverlay);
  const activeShot = shots[activeShotIndex] ?? null;

  useEffect(() => {
    setWebgl(isWebGLAvailable());
    const reduced = prefersReducedMotion();
    setReducedMotion(reduced);
    if (reduced) {
      setOverlay("arcs", true);
      usePlayback.getState().pause();
    }
  }, [setOverlay]);

  const liveText = useMemo(() => {
    if (!activeShot) {
      return `3D court visualization for ${homeName} versus ${awayName}.`;
    }
    return (
      `Current shot ${activeShot.shotIndex + 1}: ${formatShotType(activeShot.shotType)}, ` +
      `${Math.round(activeShot.launchSpeedKmh)} kilometres per hour, ` +
      `${activeShot.hitter.toLowerCase()} hitter.`
    );
  }, [activeShot, awayName, homeName]);

  const use3d = webgl === true && !forceFallback;

  return (
    <div className={cn("flex min-h-[320px] flex-1 flex-col", className)}>
      {use3d ? (
        <div className="flex flex-wrap items-center gap-2 border-b border-hairline bg-white px-3 py-2">
          <button
            type="button"
            onClick={toggle}
            className="border border-foreground bg-foreground px-2.5 py-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-background"
          >
            {playing ? "Pause" : "Play"}
          </button>
          <div className="flex flex-wrap gap-1" role="group" aria-label="Camera presets">
            {PRESETS.map((id) => (
              <button
                key={id}
                type="button"
                onClick={() => setCameraPreset(id)}
                className={
                  cameraPreset === id
                    ? "border border-foreground bg-foreground px-2 py-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-background"
                    : "border border-hairline bg-white px-2 py-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
                }
              >
                {CAMERA_PRESET_LABELS[id]}
              </button>
            ))}
          </div>
          <div className="flex flex-wrap gap-1" role="group" aria-label="Court overlays">
            {OVERLAY_KEYS.map(({ key, label }) => (
              <button
                key={key}
                type="button"
                onClick={() => toggleOverlay(key)}
                aria-pressed={overlays[key]}
                className={
                  overlays[key]
                    ? "border border-foreground bg-foreground px-2 py-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-background"
                    : "border border-hairline bg-white px-2 py-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
                }
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      ) : null}
      <div className="relative min-h-[280px] flex-1 overflow-hidden">
        {webgl === null ? (
          <div className="flex h-full min-h-[280px] items-center justify-center bg-[#0b5c2e] font-sans text-xs font-semibold uppercase tracking-wide text-white/80">
            Loading court…
          </div>
        ) : use3d ? (
          <CourtViz
            surface={surface}
            cameraPreset={cameraPreset}
            animatePresets={!reducedMotion}
            className="min-h-[280px] h-full w-full aspect-video lg:aspect-auto lg:min-h-[420px]"
            label={liveText}
            onError={() => setForceFallback(true)}
          />
        ) : (
          <CourtTopDownFallback homeName={homeName} awayName={awayName} className="h-full min-h-[280px]" />
        )}
        {use3d && activeShot ? (
          <aside className="pointer-events-none absolute right-2 top-2 border border-hairline bg-white/95 px-2.5 py-1.5 shadow-sm">
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.12em] text-muted-foreground">
              Shot {activeShot.shotIndex + 1}
            </p>
            <p className="font-display text-xs font-semibold text-foreground">
              {formatShotType(activeShot.shotType)}
            </p>
            <p className="font-sans text-[11px] tabular-nums text-muted-foreground">
              {Math.round(activeShot.launchSpeedKmh)} km/h
            </p>
          </aside>
        ) : null}
      </div>
      <p className="sr-only" aria-live="polite">
        {liveText}
        {reducedMotion ? " Reduced motion is on; playback stays paused until you press play." : ""}
      </p>
      {webgl === false || forceFallback ? null : (
        <button
          type="button"
          className="sr-only focus:not-sr-only focus:absolute focus:bottom-2 focus:left-2 focus:z-10 focus:border focus:border-foreground focus:bg-white focus:px-2 focus:py-1 focus:font-sans focus:text-[11px]"
          onClick={() => setForceFallback(true)}
        >
          Use 2D court instead
        </button>
      )}
    </div>
  );
}
