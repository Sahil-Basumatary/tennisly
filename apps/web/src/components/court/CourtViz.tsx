"use client";

import { useEffect, useRef, useState } from "react";
import type { Surface } from "@/types/replay";
import { cn } from "@/lib/utils";
import type { CameraPresetId } from "./scene/cameraPresets";
import type { CourtScene } from "./scene/CourtScene";
import type { PlayerGender } from "./scene/loadPlayer";

type CourtVizProps = {
  surface?: Surface;
  cameraPreset?: CameraPresetId;
  homeGender?: PlayerGender;
  awayGender?: PlayerGender;
  /** UUID → live replay-service via BFF. */
  matchId?: string;
  animatePresets?: boolean;
  className?: string;
  label?: string;
  onError?: (message: string) => void;
  onReplayUnavailable?: () => void;
};

export function CourtViz({
  surface = "GRASS",
  cameraPreset = "tv",
  homeGender = "male",
  awayGender = "male",
  matchId,
  animatePresets = true,
  className,
  label = "3D court visualization",
  onError,
  onReplayUnavailable,
}: CourtVizProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const sceneRef = useRef<CourtScene | null>(null);
  const skipPresetAnim = useRef(true);
  const onErrorRef = useRef(onError);
  const onReplayUnavailableRef = useRef(onReplayUnavailable);
  onErrorRef.current = onError;
  onReplayUnavailableRef.current = onReplayUnavailable;
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let disposed = false;
    let observer: ResizeObserver | null = null;
    skipPresetAnim.current = true;

    void (async () => {
      try {
        const { CourtScene } = await import("./scene/CourtScene");
        if (disposed) return;
        const instance = new CourtScene({
          canvas,
          surface,
          cameraPreset,
          homeGender,
          awayGender,
          matchId,
          onReplayUnavailable: () => onReplayUnavailableRef.current?.(),
        });
        sceneRef.current = instance;
        setReady(true);
        observer = new ResizeObserver(() => instance.resize());
        observer.observe(canvas.parentElement ?? canvas);
      } catch (err) {
        if (!disposed) {
          const message = err instanceof Error ? err.message : "WebGL unavailable";
          setError(message);
          onErrorRef.current?.(message);
        }
      }
    })();

    return () => {
      disposed = true;
      observer?.disconnect();
      sceneRef.current?.dispose();
      sceneRef.current = null;
      setReady(false);
    };
  }, [surface, homeGender, awayGender, matchId]);

  useEffect(() => {
    if (!ready || !sceneRef.current) return;
    if (skipPresetAnim.current) {
      skipPresetAnim.current = false;
      return;
    }
    sceneRef.current.setCameraPreset(cameraPreset, animatePresets);
  }, [cameraPreset, ready, animatePresets]);

  return (
    <div
      className={cn(
        "relative isolate min-h-[320px] w-full overflow-hidden bg-[#0b5c2e]",
        className,
      )}
    >
      <canvas
        ref={canvasRef}
        className="absolute inset-0 h-full w-full touch-none"
        role="img"
        aria-label={label}
      />
      {!ready && !error ? (
        <p className="absolute inset-0 flex items-center justify-center font-sans text-xs font-semibold uppercase tracking-wide text-white/80">
          Loading court…
        </p>
      ) : null}
      {error ? (
        <p className="absolute inset-0 flex items-center justify-center px-4 text-center font-sans text-sm text-white">
          Court visualization unavailable. {error}
        </p>
      ) : null}
    </div>
  );
}
