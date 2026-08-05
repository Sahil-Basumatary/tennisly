import { create } from "zustand";
import type { PointSummary, ShotSummary } from "@/types/replay";

type OverlayFlags = {
  arcs: boolean;
  landings: boolean;
  serveBox: boolean;
  heatmapHome: boolean;
  heatmapAway: boolean;
};

type ReplaySessionState = {
  shots: ShotSummary[];
  points: PointSummary[];
  /** First-frame clock time for each shotIndex, for transport stepping. */
  shotStartTimes: number[];
  /** First-frame clock time per point (aligned to `points` order). */
  pointStartTimes: number[];
  activeShotIndex: number;
  overlays: OverlayFlags;
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

export const useReplaySession = create<ReplaySessionState>((set, get) => ({
  shots: [],
  points: [],
  shotStartTimes: [],
  pointStartTimes: [],
  activeShotIndex: 0,
  overlays: { ...DEFAULT_OVERLAYS },
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
      shots: [],
      points: [],
      shotStartTimes: [],
      pointStartTimes: [],
      activeShotIndex: 0,
      overlays: { ...DEFAULT_OVERLAYS },
    }),
}));
