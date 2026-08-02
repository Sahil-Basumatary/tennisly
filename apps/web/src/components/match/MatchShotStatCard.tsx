"use client";

import { formatShotType } from "@/lib/shot-labels";
import { landingIsIn } from "@/lib/court-bounds";
import { useReplaySession } from "@/stores/replaySession";

/**
 * Deeper point readout for the stats rail — canvas keeps the lean shot chip.
 */
export function MatchShotStatCard() {
  const shots = useReplaySession((s) => s.shots);
  const activeShotIndex = useReplaySession((s) => s.activeShotIndex);
  const shot = shots[activeShotIndex];

  if (!shot) {
    return (
      <div className="mt-5 border-t border-hairline pt-4">
        <p className="mb-1 font-sans text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
          This point
        </p>
        <p className="font-sans text-xs text-muted-foreground">Waiting for rally data…</p>
      </div>
    );
  }

  const call = landingIsIn(shot.shotType, shot.landing, shot.hitter) ? "IN" : "OUT";

  return (
    <div className="mt-5 border-t border-hairline pt-4" aria-live="polite">
      <p className="mb-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
        This point · shot {shot.shotIndex + 1}
      </p>
      <p className="font-display text-sm font-semibold text-foreground">
        {formatShotType(shot.shotType)}
      </p>
      <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1.5 font-data text-[12px]">
        <div className="flex justify-between gap-2 border-b border-hairline pb-1">
          <dt className="text-muted-foreground">Speed</dt>
          <dd className="font-semibold tabular-nums">{Math.round(shot.launchSpeedKmh)} km/h</dd>
        </div>
        <div className="flex justify-between gap-2 border-b border-hairline pb-1">
          <dt className="text-muted-foreground">Call</dt>
          <dd className="font-semibold uppercase">{call}</dd>
        </div>
        <div className="flex justify-between gap-2 border-b border-hairline pb-1">
          <dt className="text-muted-foreground">Apex</dt>
          <dd className="font-semibold tabular-nums">{shot.apexHeightMetres.toFixed(1)} m</dd>
        </div>
        <div className="flex justify-between gap-2 border-b border-hairline pb-1">
          <dt className="text-muted-foreground">Flight</dt>
          <dd className="font-semibold tabular-nums">{shot.flightSeconds.toFixed(2)} s</dd>
        </div>
        <div className="col-span-2 flex justify-between gap-2">
          <dt className="text-muted-foreground">Spin · hitter</dt>
          <dd className="font-semibold uppercase">
            {shot.spin} · {shot.hitter}
          </dd>
        </div>
      </dl>
    </div>
  );
}
