"use client";

import dynamic from "next/dynamic";
import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import type { Surface } from "@/types/replay";
import {
  CAMERA_PRESET_LABELS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "@/components/court/scene/cameraPresets";
import { CallStamp } from "@/components/court/controls/CallStamp";
import { OverlayChipGroup } from "@/components/court/controls/OverlayChipGroup";
import { ScoreBug } from "@/components/court/controls/ScoreBug";
import { SegmentedControl } from "@/components/court/controls/SegmentedControl";
import { SynthesizedBadge } from "@/components/court/controls/SynthesizedBadge";
import { TransportBar } from "@/components/court/controls/TransportBar";
import type { PlayerGender } from "@/components/court/scene/loadPlayer";
import { useReplayHotkeys } from "@/hooks/useReplayHotkeys";
import { bounceCallAtTime } from "@/lib/bounce-call";
import { formatShotType } from "@/lib/shot-labels";
import { usePlayback } from "@/stores/playback";
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

const SURFACE_OPTIONS = [
  { id: "GRASS", label: "Grass" },
  { id: "CLAY", label: "Clay" },
  { id: "HARD", label: "Hard" },
] as const;

const TOUR_OPTIONS = [
  { id: "men", label: "Men" },
  { id: "women", label: "Women" },
  { id: "mixed", label: "Mixed" },
] as const;

const CAMERA_OPTIONS = (Object.keys(CAMERA_PRESET_LABELS) as CameraPresetId[]).map((id) => ({
  id,
  label: CAMERA_PRESET_LABELS[id],
}));

const OVERLAY_OPTIONS = [
  { key: "arcs", label: "Arcs" },
  { key: "landings", label: "Landings" },
  { key: "serveBox", label: "Serve box" },
  { key: "heatmapHome", label: "Home heat" },
  { key: "heatmapAway", label: "Away heat" },
] as const;

type TourLine = (typeof TOUR_OPTIONS)[number]["id"];

const TOUR_GENDERS: Record<TourLine, { home: PlayerGender; away: PlayerGender }> = {
  men: { home: "male", away: "male" },
  women: { home: "female", away: "female" },
  mixed: { home: "male", away: "female" },
};

function CourtPreviewInner() {
  const searchParams = useSearchParams();
  const matchId = searchParams.get("matchId") ?? undefined;
  const [surface, setSurface] = useState<Surface>("GRASS");
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const [tour, setTour] = useState<TourLine>("men");
  const [replayUnavailable, setReplayUnavailable] = useState(false);
  const genders = useMemo(() => TOUR_GENDERS[tour], [tour]);
  const shots = useReplaySession((s) => s.shots);
  const points = useReplaySession((s) => s.points);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const shotStarts = useReplaySession((s) => s.shotStartTimes);
  const overlays = useReplaySession((s) => s.overlays);
  const toggleOverlay = useReplaySession((s) => s.toggleOverlay);
  const timeSeconds = usePlayback((s) => s.timeSeconds);
  const activeShot = shots[activeShotIndex] ?? null;
  const callStamp = useMemo(
    () => bounceCallAtTime(activeShot, timeSeconds, shotStarts[activeShotIndex] ?? 0),
    [activeShot, activeShotIndex, shotStarts, timeSeconds],
  );
  useReplayHotkeys({ enabled: Boolean(matchId) && !replayUnavailable });

  if (!matchId) {
    return (
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <header className="mb-6">
          <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
            Court viz · broadcast preview
          </p>
          <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
            Broadcast court scene
          </h1>
        </header>
        <p className="border border-hairline bg-white px-4 py-10 text-center font-sans text-sm text-muted-foreground">
          Pass a real match UUID as <code className="font-data">?matchId=</code> to load a live
          replay. Fake rallies are not served.
        </p>
      </main>
    );
  }

  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <header className="mb-6">
        <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
          Court viz · broadcast preview
        </p>
        <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
          Broadcast court scene
        </h1>
        <p className="mt-1 max-w-2xl font-sans text-sm text-muted-foreground">
          Live replay for match <span className="font-data">{matchId}</span>.
        </p>
      </header>
      <div className="mb-4 flex flex-wrap items-start gap-x-8 gap-y-4 border border-hairline bg-white px-4 py-3.5">
        <SegmentedControl label="Surface" options={SURFACE_OPTIONS} value={surface} onChange={setSurface} />
        <SegmentedControl label="Tour" options={TOUR_OPTIONS} value={tour} onChange={setTour} />
      </div>
      <div
        className="relative overflow-hidden border border-hairline bg-black"
        tabIndex={0}
        aria-keyshortcuts="Space, ArrowLeft, ArrowRight, Shift+ArrowLeft, Shift+ArrowRight, Digit1, Digit2, Digit3, Digit4, KeyJ, KeyL"
      >
        <CourtViz
          key={`${surface}-${genders.home}-${genders.away}-${matchId}`}
          surface={surface}
          cameraPreset={cameraPreset}
          homeGender={genders.home}
          awayGender={genders.away}
          matchId={matchId}
          className="min-h-[70vh] aspect-video"
          label={`${surface.toLowerCase()} tennis court in 3D`}
          onReplayUnavailable={() => setReplayUnavailable(true)}
        />
        {replayUnavailable ? (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center bg-black/60 px-6">
            <p className="max-w-md text-center font-sans text-sm font-semibold text-white">
              Replay is not available for this match yet.
            </p>
          </div>
        ) : (
          <>
            <div className="pointer-events-none absolute inset-x-0 top-0 bg-gradient-to-b from-black/75 via-black/30 to-transparent px-3 pb-10 pt-3">
              <div className="pointer-events-auto flex flex-wrap items-start gap-x-6 gap-y-3">
                <SegmentedControl
                  label="Camera"
                  options={CAMERA_OPTIONS}
                  value={cameraPreset}
                  onChange={setCameraPreset}
                  tone="dark"
                />
                <OverlayChipGroup
                  label="Overlays"
                  options={OVERLAY_OPTIONS}
                  values={overlays}
                  onToggle={toggleOverlay}
                  tone="dark"
                />
              </div>
            </div>
            <SynthesizedBadge className="top-3 right-3" />
            <ScoreBug
              className="top-16"
              status={points.length > 0 ? "final" : "upcoming"}
              home={{ name: "Home", sets: [], games: 0, points: "0", serving: true }}
              away={{ name: "Away", sets: [], games: 0, points: "0" }}
            />
            <TransportBar />
            {callStamp ? (
              <CallStamp key={`${activeShotIndex}-${callStamp}`} call={callStamp} />
            ) : null}
            {activeShot ? (
              <aside
                className="pointer-events-none absolute right-3 top-16 border-l-2 border-primary bg-black/75 px-3 py-2 text-white backdrop-blur-sm"
                aria-live="polite"
              >
                <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.14em] text-white/60">
                  Shot {activeShot.shotIndex + 1}
                </p>
                <p className="font-display text-sm font-semibold">
                  {formatShotType(activeShot.shotType)}
                </p>
                <p className="mt-0.5 font-data text-xs tabular-nums text-white/80">
                  {Math.round(activeShot.launchSpeedKmh)} km/h · {activeShot.hitter.toLowerCase()}
                </p>
              </aside>
            ) : null}
          </>
        )}
      </div>
      <p className="sr-only">
        Keyboard: Space play or pause. Left and right arrows step shots. Shift plus arrows step
        points. Keys 1 to 4 set speed. J and L seek one second.
      </p>
    </main>
  );
}

export default function CourtPreviewPage() {
  return (
    <Suspense
      fallback={
        <main className="mx-auto flex min-h-[50vh] max-w-6xl items-center justify-center px-4">
          <p className="font-sans text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Loading preview…
          </p>
        </main>
      }
    >
      <CourtPreviewInner />
    </Suspense>
  );
}
