"use client";

import dynamic from "next/dynamic";
import { useCallback, useMemo, useState } from "react";
import type { Surface } from "@/types/replay";
import type { MatchCentrePanel } from "@/types/scaffolds";
import {
  CAMERA_PRESET_LABELS,
  DEFAULT_CAMERA_PRESET,
  type CameraPresetId,
} from "@/components/court/cameraPresetIds";
import { CourtReplay2D } from "@/components/court/CourtReplay2D";
import { CallStamp } from "@/components/court/controls/CallStamp";
import { OverlayChipGroup } from "@/components/court/controls/OverlayChipGroup";
import { ReplayStatsOverlay } from "@/components/court/controls/ReplayStatsOverlay";
import { ScoreBug } from "@/components/court/controls/ScoreBug";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import { SynthesizedBadge } from "@/components/court/controls/SynthesizedBadge";
import { TransportBar } from "@/components/court/controls/TransportBar";
import { useReducedMotion, useWebGLSupport } from "@/hooks/useClientCapabilities";
import { useReplayDriver } from "@/hooks/useReplayDriver";
import { useReplayHotkeys } from "@/hooks/useReplayHotkeys";
import { bounceCallAtTime } from "@/lib/bounce-call";
import { indexAtOrBefore } from "@/lib/replay-transport";
import { scoreFromSnapshot } from "@/lib/score-snapshot";
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
  homePhotoUrl?: string | null;
  awayPhotoUrl?: string | null;
  score: MatchCentrePanel["score"];
  status: MatchCentrePanel["status"];
  surface?: Surface;
  matchId?: string;
  homePlayerId?: string;
  awayPlayerId?: string;
  className?: string;
};

export function MatchCourtPanel({
  homeName,
  awayName,
  homePhotoUrl,
  awayPhotoUrl,
  score,
  status,
  surface = "GRASS",
  matchId,
  homePlayerId,
  awayPlayerId,
  className,
}: MatchCourtPanelProps) {
  const [view, setView] = useState<"2d" | "3d">("2d");
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const live = status === "live";
  const { status: replayStatus, connection } = useReplayDriver({
    matchId,
    enabled: Boolean(matchId),
    live,
    loop: !live,
  });
  const replayUnavailable = replayStatus === "unavailable";
  const webgl = useWebGLSupport();
  const reducedMotion = useReducedMotion();
  const shots = useReplaySession((s) => s.shots);
  const points = useReplaySession((s) => s.points);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const shotStarts = useReplaySession((s) => s.shotStartTimes);
  const pointStarts = useReplaySession((s) => s.pointStartTimes);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const activeShot = shots[activeShotIndex] ?? null;
  const onVizError = useCallback(() => setView("2d"), []);
  const callStamp = useMemo(
    () => bounceCallAtTime(activeShot, timeSeconds, shotStarts[activeShotIndex] ?? 0),
    [activeShot, activeShotIndex, shotStarts, timeSeconds],
  );
  const tapeScore = useMemo(() => {
    const pointIndex = pointStarts.length > 0 ? indexAtOrBefore(pointStarts, timeSeconds) : 0;
    return scoreFromSnapshot(points[pointIndex]?.scoreSnapshot, score, homePlayerId, awayPlayerId);
  }, [awayPlayerId, homePlayerId, pointStarts, points, score, timeSeconds]);
  const use3d = view === "3d" && webgl === true;
  useReplayHotkeys({ enabled: !replayUnavailable && replayStatus === "ready" });

  const liveText = useMemo(() => {
    if (replayUnavailable) {
      return `The court replay is not ready for ${homeName} versus ${awayName}.`;
    }
    if (!activeShot) {
      return `Court replay for ${homeName} versus ${awayName}.`;
    }
    return (
      `Current shot ${activeShot.shotIndex + 1}: ${formatShotType(activeShot.shotType)}, ` +
      `${Math.round(activeShot.launchSpeedKmh)} kilometres per hour, ` +
      `${activeShot.hitter.toLowerCase()} hitter.`
    );
  }, [activeShot, awayName, homeName, replayUnavailable]);

  return (
    <div
      className={cn("flex min-h-[320px] flex-1 flex-col", className)}
      tabIndex={0}
      aria-keyshortcuts="Space, ArrowLeft, ArrowRight, Shift+ArrowLeft, Shift+ArrowRight, Digit1, Digit2, Digit3, Digit4, KeyJ, KeyL"
    >
      <div className="relative min-h-[280px] flex-1 overflow-hidden">
        {use3d ? (
          <CourtViz
            surface={surface}
            cameraPreset={cameraPreset}
            animatePresets={!reducedMotion}
            className="min-h-[280px] h-full w-full aspect-video lg:aspect-auto lg:min-h-[420px]"
            label={liveText}
            onError={onVizError}
          />
        ) : (
          <CourtReplay2D
            surface={surface}
            homeName={homeName}
            awayName={awayName}
            className="h-full min-h-[280px]"
            label={liveText}
          />
        )}
        {replayUnavailable ? (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center bg-black/55 px-6">
            <p className="max-w-sm text-center font-sans text-sm font-semibold text-white">
              The court replay is not ready for this match.
            </p>
          </div>
        ) : (
          <>
            {use3d ? (
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
            ) : null}
            <SynthesizedBadge />
            {live ? (
              <p
                className={cn(
                  "pointer-events-none absolute right-2 top-8 bg-black/75 px-2 py-0.5 font-sans text-[9px] font-bold uppercase tracking-[0.16em] text-white/85",
                  connection === "reconnecting" && "text-amber-300",
                )}
              >
                {connection === "reconnecting" ? "Reconnecting" : "Live updates"}
              </p>
            ) : null}
            <ScoreBug
              status={status}
              home={{
                name: homeName,
                photoUrl: homePhotoUrl,
                sets: tapeScore.homeSets,
                games: tapeScore.homeGames,
                points: tapeScore.homePoints,
                serving: tapeScore.server === "HOME",
              }}
              away={{
                name: awayName,
                photoUrl: awayPhotoUrl,
                sets: tapeScore.awaySets,
                games: tapeScore.awayGames,
                points: tapeScore.awayPoints,
                serving: tapeScore.server === "AWAY",
              }}
              className={use3d ? "top-14" : "top-2"}
            />
            {homePlayerId && awayPlayerId ? (
              <ReplayStatsOverlay
                homePlayerId={homePlayerId}
                awayPlayerId={awayPlayerId}
                homeLabel={homeName.slice(0, 3).toUpperCase()}
                awayLabel={awayName.slice(0, 3).toUpperCase()}
                className={use3d ? "top-32 sm:top-36" : "top-24 sm:top-28"}
              />
            ) : null}
            {callStamp ? (
              <CallStamp key={`${activeShotIndex}-${callStamp}`} call={callStamp} />
            ) : null}
            <TransportBar />
            {activeShot ? (
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
          </>
        )}
      </div>
      <div className="flex items-center justify-between gap-3 border-t border-hairline px-3 py-2">
        <p className="font-sans text-[11px] text-muted-foreground">
          The score is real. The court view is an estimate.
        </p>
        {webgl === true && view === "2d" ? (
          <button
            type="button"
            className="font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:underline"
            onClick={() => setView("3d")}
          >
            Open 3D court
          </button>
        ) : null}
        {view === "3d" ? (
          <button
            type="button"
            className="font-sans text-[11px] font-semibold uppercase tracking-wide text-foreground hover:underline"
            onClick={() => setView("2d")}
          >
            Use 2D court
          </button>
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
    </div>
  );
}
