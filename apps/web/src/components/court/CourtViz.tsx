"use client";

import { useEffect, useRef, useState } from "react";
import type { Surface } from "@/types/replay";
import { cn } from "@/lib/utils";

type CourtVizProps = {
  surface?: Surface;
  className?: string;
  label?: string;
};

export function CourtViz({
  surface = "GRASS",
  className,
  label = "3D court visualization",
}: CourtVizProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let disposed = false;
    let scene: { resize: () => void; dispose: () => void } | null = null;
    let observer: ResizeObserver | null = null;

    void (async () => {
      try {
        const { CourtScene } = await import("./scene/CourtScene");
        if (disposed) return;
        const instance = new CourtScene({ canvas, surface });
        scene = instance;
        setReady(true);
        observer = new ResizeObserver(() => instance.resize());
        observer.observe(canvas.parentElement ?? canvas);
      } catch (err) {
        if (!disposed) {
          setError(err instanceof Error ? err.message : "WebGL unavailable");
        }
      }
    })();

    return () => {
      disposed = true;
      observer?.disconnect();
      scene?.dispose();
    };
  }, [surface]);

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
