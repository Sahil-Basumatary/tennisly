import { create } from "zustand";
import type { ShotSummary } from "@/types/replay";

type OverlayFlags = {
  arcs: boolean;
  landings: boolean;
  serveBox: boolean;
};

type ReplaySessionState = {
  shots: ShotSummary[];
  activeShotIndex: number;
  overlays: OverlayFlags;
  setShots: (shots: ShotSummary[]) => void;
  setActiveShotIndex: (index: number) => void;
  setOverlay: (key: keyof OverlayFlags, enabled: boolean) => void;
  toggleOverlay: (key: keyof OverlayFlags) => void;
  reset: () => void;
};

const DEFAULT_OVERLAYS: OverlayFlags = {
  arcs: true,
  landings: true,
  serveBox: true,
};

export const useReplaySession = create<ReplaySessionState>((set, get) => ({
  shots: [],
  activeShotIndex: 0,
  overlays: { ...DEFAULT_OVERLAYS },
  setShots: (shots) => set({ shots, activeShotIndex: 0 }),
  setActiveShotIndex: (index) => {
    if (get().activeShotIndex === index) return;
    set({ activeShotIndex: index });
  },
  setOverlay: (key, enabled) =>
    set((s) => ({ overlays: { ...s.overlays, [key]: enabled } })),
  toggleOverlay: (key) =>
    set((s) => ({ overlays: { ...s.overlays, [key]: !s.overlays[key] } })),
  reset: () => set({ shots: [], activeShotIndex: 0, overlays: { ...DEFAULT_OVERLAYS } }),
}));
