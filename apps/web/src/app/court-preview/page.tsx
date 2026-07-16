"use client";

import dynamic from "next/dynamic";
import { useState } from "react";
import type { Surface } from "@/types/replay";

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

const SURFACES: Surface[] = ["GRASS", "CLAY", "HARD"];

export default function CourtPreviewPage() {
  const [surface, setSurface] = useState<Surface>("GRASS");

  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <header className="mb-6">
        <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
          Court viz · M2 preview
        </p>
        <h1 className="font-display text-2xl font-semibold text-foreground sm:text-3xl">
          Broadcast court scene
        </h1>
        <p className="mt-1 max-w-2xl font-sans text-sm text-muted-foreground">
          Full-realism Babylon scene for audit. Orbit with drag, zoom with scroll.
          Camera presets arrive in M3.
        </p>
      </header>
      <div className="mb-4 flex flex-wrap gap-2">
        {SURFACES.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setSurface(s)}
            className={
              surface === s
                ? "border border-foreground bg-foreground px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-background"
                : "border border-hairline bg-white px-3 py-1.5 font-sans text-xs font-semibold uppercase tracking-wide text-foreground hover:border-foreground"
            }
          >
            {s.toLowerCase()}
          </button>
        ))}
      </div>
      <div className="overflow-hidden border border-hairline">
        <CourtViz
          key={surface}
          surface={surface}
          className="min-h-[70vh] aspect-video"
          label={`${surface.toLowerCase()} tennis court in 3D`}
        />
      </div>
    </main>
  );
}
