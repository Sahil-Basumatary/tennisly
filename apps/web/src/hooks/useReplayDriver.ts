"use client";

import { useEffect, useState } from "react";
import { LIVE_POINT_BUFFER, tapeDurationSeconds } from "@/lib/replay-tape";
import { interpolateAtTime } from "@/lib/replay-space";
import { getMatchReplay } from "@/services/replay";
import { useLiveReplaySession, type LiveConnection } from "@/hooks/useLiveReplaySession";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

export type ReplayDriverStatus = "idle" | "loading" | "ready" | "unavailable";

type UseReplayDriverOptions = {
  matchId?: string;
  /** Homepage waits for Play before hitting replay-service. */
  enabled?: boolean;
  live?: boolean;
  loop?: boolean;
  rootRef?: { current: HTMLElement | null };
};

function syncActiveShotFromClock(): void {
  const { timeSeconds } = usePlayback.getState();
  const { frames, setActiveShotIndex } = useReplaySession.getState();
  const pose = interpolateAtTime(frames, timeSeconds);
  if (pose) setActiveShotIndex(pose.shotIndex);
}

/**
 * Owns replay fetch, hydration, and the playback clock so 2D and 3D stay
 * on one timeline. Babylon must not tick or fetch on its own.
 */
export function useReplayDriver({
  matchId,
  enabled = true,
  live = false,
  loop,
  rootRef,
}: UseReplayDriverOptions): { status: ReplayDriverStatus; connection: LiveConnection } {
  const replayKey = enabled && matchId ? `${matchId}:${live ? "live" : "replay"}` : null;
  const [loadState, setLoadState] = useState<{
    key: string;
    status: "ready" | "unavailable";
  } | null>(null);
  const status: ReplayDriverStatus = !replayKey
    ? "idle"
    : loadState?.key === replayKey
      ? loadState.status
      : "loading";
  const [inView, setInView] = useState(true);
  const playing = usePlayback((s) => s.playing);
  const { connection } = useLiveReplaySession({
    matchId,
    enabled: Boolean(enabled && live && status === "ready" && matchId),
  });

  useEffect(() => {
    const node = rootRef?.current;
    if (!node || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver(
      ([entry]) => setInView(Boolean(entry?.isIntersecting)),
      { threshold: 0.12 },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [rootRef]);

  useEffect(() => {
    if (!replayKey || !matchId) return;
    let cancelled = false;
    void getMatchReplay(matchId).then((replay) => {
      if (cancelled) return;
      if (!replay) {
        usePlayback.getState().setDuration(0);
        useReplaySession.getState().reset();
        setLoadState({ key: replayKey, status: "unavailable" });
        return;
      }
      useReplaySession.getState().hydrateReplay(replay, live ? LIVE_POINT_BUFFER : undefined);
      const session = useReplaySession.getState();
      const playback = usePlayback.getState();
      playback.setLoop(loop ?? !live);
      playback.setDuration(
        tapeDurationSeconds({
          frames: session.frames,
          shots: session.shots,
          points: session.points,
        }),
      );
      syncActiveShotFromClock();
      setLoadState({ key: replayKey, status: "ready" });
    });
    return () => {
      cancelled = true;
      usePlayback.getState().pause();
      useReplaySession.getState().reset();
    };
  }, [matchId, replayKey, live, loop]);

  useEffect(() => {
    return usePlayback.subscribe((state, prev) => {
      if (state.timeSeconds === prev.timeSeconds) return;
      syncActiveShotFromClock();
    });
  }, []);

  useEffect(() => {
    if (!playing || status !== "ready") return;
    let raf = 0;
    let last = performance.now();
    const tick = (now: number) => {
      raf = requestAnimationFrame(tick);
      const dt = Math.min((now - last) / 1000, 0.1);
      last = now;
      if (document.visibilityState !== "visible" || !inView) return;
      usePlayback.getState().tick(dt);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [playing, status, inView]);

  return { status, connection };
}
