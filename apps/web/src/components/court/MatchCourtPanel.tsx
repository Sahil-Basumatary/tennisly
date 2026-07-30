"use client";

import dynamic from "next/dynamic";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { Surface } from "@/types/replay";
import {
  CAMERA_PRESET_LABELS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "@/components/court/scene/cameraPresets";
import { CourtTopDownFallback } from "@/components/court/CourtTopDownFallback";
import { OverlayChipGroup } from "@/components/court/controls/OverlayChipGroup";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import { TransportBar } from "@/components/court/controls/TransportBar";
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

const CAMERA_OPTIONS = (Object.keys(CAMERA_PRESET_LABELS) as CameraPresetId[]).map((id) => ({
  id,
  label: CAMERA_PRESET_LABELS[id],
}));

const OVERLAY_OPTIONS = [
  { key: "arcs", label: "Arcs" },
  { key: "landings", label: "Marks" },
  { key: "heatmapHome", label: "Home" },
  { key: "heatmapAway", label: "Away" },
] as const;

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
  const shots = useReplaySession((s) => s.shots);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const setOverlay = useReplaySession((s) => s.setOverlay);
  const activeShot = shots[activeShotIndex] ?? null;
  const onVizError = useCallback(() => setForceFallback(true), []);

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
        <div className="flex flex-wrap items-start gap-x-6 gap-y-2 border-b border-hairline bg-white px-3 py-2.5">
          <SegmentedControl
            label="Camera"
            options={CAMERA_OPTIONS}
            value={cameraPreset}
            onChange={setCameraPreset}
            size="sm"
          />
          <OverlayChipGroup
            label="Overlays"
            options={OVERLAY_OPTIONS}
            values={overlays}
            onToggle={toggleOverlay}
            size="sm"
          />
        </div>
      ) : null}
      <div className="relative min-h-[280px] flex-1 overflow-hidden">
        {webgl === null ? (
          <div className="flex h-full min-h-[280px] items-center justify-center bg-[#0b5c2e] font-sans text-xs font-semibold uppercase tracking-wide text-white/80">
            Loading court…
          </div>
        ) : use3d ? (
          <>
            <CourtViz
              surface={surface}
              cameraPreset={cameraPreset}
              animatePresets={!reducedMotion}
              className="min-h-[280px] h-full w-full aspect-video lg:aspect-auto lg:min-h-[420px]"
              label={liveText}
              onError={onVizError}
            />
            <TransportBar />
          </>
        ) : (
          <CourtTopDownFallback homeName={homeName} awayName={awayName} className="h-full min-h-[280px]" />
        )}
        {use3d && activeShot ? (
          <aside className="pointer-events-none absolute right-2 top-2 border-l-2 border-primary bg-black/75 px-2.5 py-1.5 text-white backdrop-blur-sm">
            <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.12em] text-white/60">
              Shot {activeShot.shotIndex + 1}
            </p>
            <p className="font-display text-xs font-semibold">
              {formatShotType(activeShot.shotType)}
            </p>
            <p className="font-data text-[11px] tabular-nums text-white/80">
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
