"use client";

import dynamic from "next/dynamic";
import { useMemo, useState } from "react";
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
import { TransportBar } from "@/components/court/controls/TransportBar";
import type { PlayerGender } from "@/components/court/scene/loadPlayer";
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

export default function CourtPreviewPage() {
  const [surface, setSurface] = useState<Surface>("GRASS");
  const [cameraPreset, setCameraPreset] = useState<CameraPresetId>(DEFAULT_CAMERA_PRESET);
  const [tour, setTour] = useState<TourLine>("men");
  const genders = useMemo(() => TOUR_GENDERS[tour], [tour]);
  const shots = useReplaySession((s) => s.shots);
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
          Rally playback with athletes, shot overlays, and positioning heatmaps. Orbit freely or
          cut between broadcast cameras.
        </p>
      </header>
      <div className="mb-4 flex flex-wrap items-start gap-x-8 gap-y-4 border border-hairline bg-white px-4 py-3.5">
        <SegmentedControl label="Surface" options={SURFACE_OPTIONS} value={surface} onChange={setSurface} />
        <SegmentedControl label="Tour" options={TOUR_OPTIONS} value={tour} onChange={setTour} />
      </div>
      <div className="relative overflow-hidden border border-hairline bg-black">
        <CourtViz
          key={`${surface}-${genders.home}-${genders.away}`}
          surface={surface}
          cameraPreset={cameraPreset}
          homeGender={genders.home}
          awayGender={genders.away}
          className="min-h-[70vh] aspect-video"
          label={`${surface.toLowerCase()} tennis court in 3D`}
        />
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
        <ScoreBug
          className="top-16"
          home={{ name: "Home", sets: [6, 3], games: 4, points: "30", serving: true }}
          away={{ name: "Away", sets: [4, 6], games: 5, points: "40" }}
        />
        {callStamp ? (
          <CallStamp key={`${activeShotIndex}-${callStamp}`} call={callStamp} />
        ) : null}
        <TransportBar />
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
      </div>
    </main>
  );
}
