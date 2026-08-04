"use client";

import { useEffect } from "react";
import { PLAYBACK_SPEEDS, usePlayback } from "@/stores/playback";
import { nudgeTime, stepPoint, stepShot } from "@/lib/replay-transport";

function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  return target.isContentEditable;
}

type UseReplayHotkeysOptions = {
  enabled?: boolean;
};

/**
 * Broadcast-operator keys for the court feed. Shared with TransportBar actions.
 */
export function useReplayHotkeys({ enabled = true }: UseReplayHotkeysOptions = {}): void {
  useEffect(() => {
    if (!enabled) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (isTypingTarget(event.target) || event.metaKey || event.ctrlKey || event.altKey) {
        return;
      }

      const { toggle, setSpeed } = usePlayback.getState();

      if (event.code === "Space") {
        event.preventDefault();
        toggle();
        return;
      }

      if (event.key === "ArrowLeft") {
        event.preventDefault();
        if (event.shiftKey) stepPoint(-1);
        else stepShot(-1);
        return;
      }

      if (event.key === "ArrowRight") {
        event.preventDefault();
        if (event.shiftKey) stepPoint(1);
        else stepShot(1);
        return;
      }

      if (event.key === "j" || event.key === "J") {
        event.preventDefault();
        nudgeTime(-1);
        return;
      }

      if (event.key === "l" || event.key === "L") {
        event.preventDefault();
        nudgeTime(1);
        return;
      }

      const speedIndex = Number(event.key) - 1;
      if (speedIndex >= 0 && speedIndex < PLAYBACK_SPEEDS.length) {
        event.preventDefault();
        setSpeed(PLAYBACK_SPEEDS[speedIndex]!);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [enabled]);
}
