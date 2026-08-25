"use client";

import { useLiveReplaySession } from "@/hooks/useLiveReplaySession";

type MatchLiveBridgeProps = {
  matchId: string;
  enabled?: boolean;
};

/** Optional mount for pages that do not already run useReplayDriver({ live: true }). */
export function MatchLiveBridge({ matchId, enabled = false }: MatchLiveBridgeProps) {
  useLiveReplaySession({ matchId, enabled });
  return null;
}
