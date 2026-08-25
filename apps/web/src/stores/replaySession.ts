import { create } from "zustand";
import { indexPointStartTimes, indexShotStartTimes } from "@/lib/replay-index";
import {
  appendPointToTape,
  prepareHydratedTape,
  tapeDurationSeconds,
  type ReplayTape,
} from "@/lib/replay-tape";
import { usePlayback } from "@/stores/playback";
import type { PointReplay, PointSummary, ReplayFrame, ShotSummary } from "@/types/replay";

type OverlayFlags = {
  arcs: boolean;
  landings: boolean;
  serveBox: boolean;
  heatmapHome: boolean;
  heatmapAway: boolean;
};

type ReplaySessionState = {
  /** Dense samples shared by every renderer so Babylon does not own the tape. */
  frames: ReplayFrame[];
  shots: ShotSummary[];
  points: PointSummary[];
  /** First-frame clock time for each shotIndex, for transport stepping. */
  shotStartTimes: number[];
  /** First-frame clock time per point (aligned to `points` order). */
  pointStartTimes: number[];
  activeShotIndex: number;
  overlays: OverlayFlags;
  hydrateReplay: (replay: ReplayTape, maxPoints?: number) => void;
  appendPointReplay: (point: PointReplay) => boolean;
  setShots: (shots: ShotSummary[]) => void;
  setPoints: (points: PointSummary[]) => void;
  setShotStartTimes: (times: number[]) => void;
  setPointStartTimes: (times: number[]) => void;
  setActiveShotIndex: (index: number) => void;
  setOverlay: (key: keyof OverlayFlags, enabled: boolean) => void;
  toggleOverlay: (key: keyof OverlayFlags) => void;
  reset: () => void;
};

const DEFAULT_OVERLAYS: OverlayFlags = {
  arcs: true,
  landings: true,
  serveBox: true,
  heatmapHome: false,
  heatmapAway: false,
};

function applyTape(tape: ReplayTape) {
  return {
    frames: tape.frames,
    shots: tape.shots,
    points: tape.points,
    shotStartTimes: indexShotStartTimes(tape.frames),
    pointStartTimes: indexPointStartTimes(tape.frames, tape.points),
  };
}

export const useReplaySession = create<ReplaySessionState>((set, get) => ({
  frames: [],
  shots: [],
  points: [],
  shotStartTimes: [],
  pointStartTimes: [],
  activeShotIndex: 0,
  overlays: { ...DEFAULT_OVERLAYS },
  hydrateReplay: (replay, maxPoints) => {
    const tape = prepareHydratedTape(replay, maxPoints);
    set({ ...applyTape(tape), activeShotIndex: 0 });
  },
  appendPointReplay: (point) => {
    const next = appendPointToTape(
      { frames: get().frames, shots: get().shots, points: get().points },
      point,
    );
    if (!next) return false;
    const previousFirst = get().frames[0]?.timeSeconds ?? 0;
    const nextFirst = next.frames[0]?.timeSeconds ?? 0;
    set((s) => ({
      ...applyTape(next),
      activeShotIndex: Math.min(s.activeShotIndex, Math.max(0, next.shots.length - 1)),
    }));
    usePlayback.getState().extendDuration(tapeDurationSeconds(next));
    if (nextFirst < previousFirst - 1e-6) {
      const playback = usePlayback.getState();
      if (playback.timeSeconds < nextFirst) playback.seek(nextFirst);
    }
    return true;
  },
  setShots: (shots) => set({ shots, activeShotIndex: 0 }),
  setPoints: (points) => set({ points }),
  setShotStartTimes: (shotStartTimes) => set({ shotStartTimes }),
  setPointStartTimes: (pointStartTimes) => set({ pointStartTimes }),
  setActiveShotIndex: (index) => {
    if (get().activeShotIndex === index) return;
    set({ activeShotIndex: index });
  },
  setOverlay: (key, enabled) =>
    set((s) => ({ overlays: { ...s.overlays, [key]: enabled } })),
  toggleOverlay: (key) =>
    set((s) => ({ overlays: { ...s.overlays, [key]: !s.overlays[key] } })),
  reset: () =>
    set({
      frames: [],
      shots: [],
      points: [],
      shotStartTimes: [],
      pointStartTimes: [],
      activeShotIndex: 0,
      overlays: { ...DEFAULT_OVERLAYS },
    }),
}));
