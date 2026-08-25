"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { CourtReplay2D } from "@/components/court/CourtReplay2D";
import { ScoreBug } from "@/components/court/controls/ScoreBug";
import { SynthesizedBadge } from "@/components/court/controls/SynthesizedBadge";
import { TransportBar } from "@/components/court/controls/TransportBar";
import { useReplayDriver } from "@/hooks/useReplayDriver";
import { useReplayHotkeys } from "@/hooks/useReplayHotkeys";
import { indexAtOrBefore } from "@/lib/replay-transport";
import { scoreFromSnapshot } from "@/lib/score-snapshot";
import { cn } from "@/lib/utils";
import type { HomeReplayFeature } from "@/lib/home-replay";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";

type HomeLiveReplayProps = {
  feature: HomeReplayFeature | null;
};

function kindLabel(feature: HomeReplayFeature | null, unavailable: boolean): string {
  if (!feature || unavailable) return "Replay unavailable";
  if (feature.kind === "live") return "LIVE";
  return "LATEST REPLAY";
}

export function HomeLiveReplay({ feature }: HomeLiveReplayProps) {
  const [armed, setArmed] = useState(false);
  const rootRef = useRef<HTMLElement | null>(null);
  const live = feature?.kind === "live";
  const { status, connection } = useReplayDriver({
    matchId: feature?.matchId,
    enabled: armed && Boolean(feature?.matchId),
    live,
    loop: !live,
    rootRef,
  });
  const unavailable = status === "unavailable" || !feature;
  useReplayHotkeys({ enabled: armed && status === "ready" });
  const points = useReplaySession((s) => s.points);
  const pointStarts = useReplaySession((s) => s.pointStartTimes);
  const timeSeconds = usePlayback((s) => s.timeSeconds);

  useEffect(() => {
    if (armed && status === "ready") usePlayback.getState().play();
  }, [armed, status]);

  const tapeScore = useMemo(() => {
    const fallback = feature?.score ?? {
      homeSets: [],
      awaySets: [],
      homeGames: 0,
      awayGames: 0,
      homePoints: "0",
      awayPoints: "0",
      server: "HOME" as const,
    };
    const pointIndex = pointStarts.length > 0 ? indexAtOrBefore(pointStarts, timeSeconds) : 0;
    return scoreFromSnapshot(
      points[pointIndex]?.scoreSnapshot,
      fallback,
      feature?.homePlayerId,
      feature?.awayPlayerId,
    );
  }, [feature, pointStarts, points, timeSeconds]);

  const homeName = feature?.homeName ?? "Home";
  const awayName = feature?.awayName ?? "Away";
  const title = feature ? `${homeName} vs ${awayName}` : "Waiting on live tennis data";
  const showTransport = armed && status === "ready";

  return (
    <section ref={rootRef} className="relative isolate w-full bg-inverse-deep">
      <div className="mx-auto max-w-[1400px] px-6 pb-10 pt-8 md:px-10 md:pb-14 md:pt-12">
        <p className="font-sans text-[11px] font-bold uppercase tracking-[0.2em] text-white/70">
          Reconstructed live visualization
        </p>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-3">
          <h1 className="max-w-[22ch] font-display text-[28px] font-bold uppercase leading-[1.1] tracking-tight text-white md:text-[40px]">
            {title}
          </h1>
          <span
            className={cn(
              "font-sans text-[11px] font-bold uppercase tracking-[0.16em]",
              feature?.kind === "live" && !unavailable ? "text-[#ff3b30]" : "text-white/70",
            )}
          >
            {kindLabel(feature, unavailable && armed)}
          </span>
        </div>
        <p className="mt-2 max-w-2xl font-sans text-sm text-white/70">
          {feature
            ? `${feature.tournament} · ${feature.round}. Scores and point order are live. Ball flight is synthesized — this is not Hawk-Eye.`
            : "When a match with a point ledger is on, the reconstructed court plays here. Nothing is invented."}
        </p>
        <div className="relative mt-5 overflow-hidden border border-white/10 bg-black aspect-[3/4] sm:aspect-video">
          <CourtReplay2D
            surface={feature?.surface ?? "GRASS"}
            homeName={homeName}
            awayName={awayName}
            className="h-full min-h-[320px]"
            label={`Reconstructed live visualization. ${homeName} versus ${awayName}.`}
          />
          {feature && !armed ? (
            <button
              type="button"
              onClick={() => setArmed(true)}
              className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-black/45 text-white"
              aria-label="Play reconstructed rally"
            >
              <span className="flex h-16 w-16 items-center justify-center rounded-full border-2 border-white bg-black/50">
                <svg width="22" height="22" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                  <path d="M3 1.5 14 8 3 14.5z" />
                </svg>
              </span>
              <span className="mt-3 font-sans text-[12px] font-semibold uppercase tracking-[0.16em]">
                Play reconstructed rally
              </span>
            </button>
          ) : null}
          {armed && status === "loading" ? (
            <p className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center bg-black/40 font-sans text-xs font-semibold uppercase tracking-wide text-white">
              Loading replay…
            </p>
          ) : null}
          {armed && status === "unavailable" ? (
            <p className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center bg-black/55 px-6 text-center font-sans text-sm font-semibold text-white">
              Replay is not available for this match yet.
            </p>
          ) : null}
          {showTransport ? (
            <>
              <SynthesizedBadge />
              {live ? (
                <p
                  className={cn(
                    "pointer-events-none absolute right-2 top-8 bg-black/75 px-2 py-0.5 font-sans text-[9px] font-bold uppercase tracking-[0.16em] text-white/85",
                    connection === "reconnecting" && "text-amber-300",
                  )}
                >
                  {connection === "reconnecting" ? "Reconnecting" : "Live ledger"}
                </p>
              ) : null}
              <ScoreBug
                status={feature?.status === "live" ? "live" : "final"}
                home={{
                  name: homeName,
                  photoUrl: feature?.homePhotoUrl,
                  sets: tapeScore.homeSets,
                  games: tapeScore.homeGames,
                  points: tapeScore.homePoints,
                  serving: tapeScore.server === "HOME",
                }}
                away={{
                  name: awayName,
                  photoUrl: feature?.awayPhotoUrl,
                  sets: tapeScore.awaySets,
                  games: tapeScore.awayGames,
                  points: tapeScore.awayPoints,
                  serving: tapeScore.server === "AWAY",
                }}
              />
              <TransportBar />
            </>
          ) : feature && !armed ? (
            <ScoreBug
              status={feature.status === "live" ? "live" : "final"}
              home={{
                name: homeName,
                photoUrl: feature.homePhotoUrl,
                sets: feature.score.homeSets,
                games: feature.score.homeGames,
                points: feature.score.homePoints,
                serving: feature.score.server === "HOME",
              }}
              away={{
                name: awayName,
                photoUrl: feature.awayPhotoUrl,
                sets: feature.score.awaySets,
                games: feature.score.awayGames,
                points: feature.score.awayPoints,
                serving: feature.score.server === "AWAY",
              }}
            />
          ) : null}
        </div>
        {feature ? (
          <Link
            href={feature.href}
            className="mt-4 inline-flex font-sans text-[13px] font-semibold text-white/90 underline-offset-4 hover:underline"
          >
            Open 3D Match Centre
          </Link>
        ) : null}
      </div>
    </section>
  );
}
