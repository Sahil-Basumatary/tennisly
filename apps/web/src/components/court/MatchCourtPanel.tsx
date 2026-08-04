"use client";

import dynamic from "next/dynamic";
import { useCallback, useMemo, useState } from "react";
import type { Surface } from "@/types/replay";
import type { MatchCentrePanel } from "@/types/scaffolds";
import {
  CAMERA_PRESET_LABELS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "@/components/court/scene/cameraPresets";
import { CourtTopDownFallback } from "@/components/court/CourtTopDownFallback";
import { CallStamp } from "@/components/court/controls/CallStamp";
import { OverlayChipGroup } from "@/components/court/controls/OverlayChipGroup";
import { ReplayStatsOverlay } from "@/components/court/controls/ReplayStatsOverlay";
import { ScoreBug } from "@/components/court/controls/ScoreBug";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import { TransportBar } from "@/components/court/controls/TransportBar";
import { useReducedMotion, useWebGLSupport } from "@/hooks/useClientCapabilities";
import { useReplayHotkeys } from "@/hooks/useReplayHotkeys";
import { bounceCallAtTime } from "@/lib/bounce-call";
import { formatShotType } from "@/lib/shot-labels";
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
  { key: "serveBox", label: "Serve" },
  { key: "heatmapHome", label: "Home" },
  { key: "heatmapAway", label: "Away" },
] as const;

type MatchCourtPanelProps = {
  homeName: string;
  awayName: string;
  score: MatchCentrePanel["score"];
  status: MatchCentrePanel["status"];
  surface?: Surface;
  /** UUID → live replay-service; scaffold ids keep the mock. */
  matchId?: string;
  homePlayerId?: string;
  awayPlayerId?: string;
  className?: string;
};

/**
 * Match-centre court slot: Babylon when WebGL works, static SVG otherwise.
 * Broadcast chrome (score bug, dark feed controls, transport) sits on the feed.
 */
export function MatchCourtPanel({
  homeName,
  awayName,
  score,
  status,
  surface = "GRASS",
  matchId,
  homePlayerId,
  awayPlayerId,
  className,
}: MatchCourtPanelProps) {
  const [forceFallback, setForceFallback] = useState(false);
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const webgl = useWebGLSupport();
  const reducedMotion = useReducedMotion();
  const shots = useReplaySession((s) => s.shots);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const shotStarts = useReplaySession((s) => s.shotStartTimes);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const activeShot = shots[activeShotIndex] ?? null;
  const onVizError = useCallback(() => setForceFallback(true), []);
  const callStamp = useMemo(
    () => bounceCallAtTime(activeShot, timeSeconds, shotStarts[activeShotIndex] ?? 0),
    [activeShot, activeShotIndex, shotStarts, timeSeconds],
  );

  const use3d = webgl === true && !forceFallback;
  useReplayHotkeys({ enabled: use3d });

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

  return (
    <div
      className={cn("flex min-h-[320px] flex-1 flex-col", className)}
      tabIndex={use3d ? 0 : undefined}
      aria-keyshortcuts="Space, ArrowLeft, ArrowRight, Shift+ArrowLeft, Shift+ArrowRight, Digit1, Digit2, Digit3, Digit4, KeyJ, KeyL"
    >
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
              matchId={matchId}
              animatePresets={!reducedMotion}
              className="min-h-[280px] h-full w-full aspect-video lg:aspect-auto lg:min-h-[420px]"
              label={liveText}
              onError={onVizError}
            />
            <div className="pointer-events-none absolute inset-x-0 top-0 bg-gradient-to-b from-black/70 via-black/25 to-transparent px-2 pb-8 pt-2">
              <div className="pointer-events-auto flex flex-wrap items-start gap-x-5 gap-y-2">
                <SegmentedControl
                  label="Camera"
                  options={CAMERA_OPTIONS}
                  value={cameraPreset}
                  onChange={setCameraPreset}
                  size="sm"
                  tone="dark"
                />
                <OverlayChipGroup
                  label="Overlays"
                  options={OVERLAY_OPTIONS}
                  values={overlays}
                  onToggle={toggleOverlay}
                  size="sm"
                  tone="dark"
                />
              </div>
            </div>
            <ScoreBug
              status={status}
              home={{
                name: homeName,
                sets: score.homeSets,
                games: score.homeGames,
                points: score.homePoints,
                serving: score.server === "HOME",
              }}
              away={{
                name: awayName,
                sets: score.awaySets,
                games: score.awayGames,
                points: score.awayPoints,
                serving: score.server === "AWAY",
              }}
              className="top-14"
            />
            {homePlayerId && awayPlayerId ? (
              <ReplayStatsOverlay
                homePlayerId={homePlayerId}
                awayPlayerId={awayPlayerId}
                homeLabel={homeName.slice(0, 3).toUpperCase()}
                awayLabel={awayName.slice(0, 3).toUpperCase()}
                className="top-32 sm:top-36"
              />
            ) : null}
            {callStamp ? (
              <CallStamp key={`${activeShotIndex}-${callStamp}`} call={callStamp} />
            ) : null}
            <TransportBar />
          </>
        ) : (
          <CourtTopDownFallback homeName={homeName} awayName={awayName} className="h-full min-h-[280px]" />
        )}
        {use3d && activeShot ? (
          <aside className="pointer-events-none absolute right-2 top-14 border-l-2 border-primary bg-black/75 px-2.5 py-1.5 text-white backdrop-blur-sm">
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
        {reducedMotion ? " Reduced motion is on; camera cuts are instant and playback stays paused." : ""}
      </p>
      <p className="sr-only">
        Keyboard: Space play or pause. Left and right arrows step shots. Shift plus arrows step
        points. Keys 1 to 4 set speed. J and L seek one second.
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
