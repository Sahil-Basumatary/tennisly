import { create } from "zustand";

export const PLAYBACK_SPEEDS = [0.5, 1, 2, 4] as const;
export type PlaybackSpeed = (typeof PLAYBACK_SPEEDS)[number];

type PlaybackState = {
  playing: boolean;
  timeSeconds: number;
  durationSeconds: number;
  speed: PlaybackSpeed;
  loop: boolean;
  play: () => void;
  pause: () => void;
  toggle: () => void;
  seek: (timeSeconds: number) => void;
  setSpeed: (speed: PlaybackSpeed) => void;
  setLoop: (loop: boolean) => void;
  setDuration: (durationSeconds: number) => void;
  /** Grow the tape without scrubbing the viewer back to 0. */
  extendDuration: (durationSeconds: number) => void;
  /** Advance clock by wall-clock dt; call from the shared replay driver. */
  tick: (deltaSeconds: number) => void;
};

export const usePlayback = create<PlaybackState>((set, get) => ({
  playing: false,
  timeSeconds: 0,
  durationSeconds: 0,
  speed: 1,
  loop: true,
  play: () => set({ playing: true }),
  pause: () => set({ playing: false }),
  toggle: () => set((s) => ({ playing: !s.playing })),
  seek: (timeSeconds) => {
    const { durationSeconds } = get();
    const clamped = Math.max(0, Math.min(durationSeconds, timeSeconds));
    set({ timeSeconds: clamped });
  },
  setSpeed: (speed) => set({ speed }),
  setLoop: (loop) => set({ loop }),
  setDuration: (durationSeconds) => set({ durationSeconds, timeSeconds: 0, playing: false }),
  extendDuration: (durationSeconds) =>
    set((s) => ({
      durationSeconds: Math.max(0, durationSeconds),
      timeSeconds: Math.min(s.timeSeconds, Math.max(0, durationSeconds)),
    })),
  tick: (deltaSeconds) => {
    const { playing, timeSeconds, durationSeconds, speed, loop } = get();
    if (!playing || durationSeconds <= 0) return;
    let next = timeSeconds + deltaSeconds * speed;
    if (next >= durationSeconds) {
      if (loop) next = next % durationSeconds;
      else {
        set({ timeSeconds: durationSeconds, playing: false });
        return;
      }
    }
    set({ timeSeconds: next });
  },
}));
