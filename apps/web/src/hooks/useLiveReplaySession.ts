"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  clampJitter,
  livePollIntervalMs,
  liveTransport,
  reconnectDelayMs,
} from "@/lib/live-poll";
import type { LiveCursorDocument } from "@/lib/live-score-document";
import { isSealedPoint } from "@/lib/replay-cache-policy";
import { missingPointRange, needsEventRecovery, recoverEventCursor } from "@/lib/live-replay-sync";
import { getPointReplay } from "@/services/replay";
import { useReplaySession } from "@/stores/replaySession";

export type LiveConnection = "idle" | "live" | "reconnecting";

type UseLiveReplaySessionOptions = {
  matchId?: string;
  enabled?: boolean;
};

function stompFrame(command: string, headers: Record<string, string>, body = ""): string {
  const lines = [command, ...Object.entries(headers).map(([k, v]) => `${k}:${v}`), "", body];
  return `${lines.join("\n")}\0`;
}

function parseSequence(body: string): number | null {
  try {
    const sequence = Number(JSON.parse(body).sequence);
    return Number.isSafeInteger(sequence) && sequence > 0 ? sequence : null;
  } catch {
    return null;
  }
}

async function fetchCursor(
  matchId: string,
  etag?: string,
): Promise<{ cursor: LiveCursorDocument | null; etag?: string; notModified: boolean; ok: boolean }> {
  const headers: HeadersInit = {};
  if (etag) headers["If-None-Match"] = etag;
  const response = await fetch(`/api/matches/${matchId}/cursor`, { headers, cache: "no-store" });
  const nextTag = response.headers.get("ETag") ?? etag;
  if (response.status === 304) {
    return { cursor: null, etag: nextTag, notModified: true, ok: true };
  }
  if (!response.ok) {
    return { cursor: null, etag: nextTag, notModified: false, ok: false };
  }
  const body = (await response.json()) as LiveCursorDocument;
  return { cursor: body, etag: nextTag, notModified: false, ok: true };
}

/**
 * Public live sessions poll a compact sequence cursor. The event log remains
 * the gap-recovery plane; WebSocket is an opt-in wake-up, not the default fanout.
 */
export function useLiveReplaySession({
  matchId,
  enabled = false,
}: UseLiveReplaySessionOptions): { connection: LiveConnection } {
  const [connection, setConnection] = useState<LiveConnection>("idle");
  const syncingRef = useRef(false);
  const cursorRef = useRef(0);
  const pointsPlayedRef = useRef(0);
  const etagRef = useRef<string | undefined>(undefined);
  const failuresRef = useRef(0);

  const syncFromCursor = useCallback(async () => {
    if (!matchId || syncingRef.current) return;
    syncingRef.current = true;
    try {
      const result = await fetchCursor(matchId, etagRef.current);
      if (result.etag) etagRef.current = result.etag;
      if (!result.ok) {
        failuresRef.current += 1;
        setConnection("reconnecting");
        return;
      }
      failuresRef.current = 0;
      setConnection("live");
      if (result.notModified || !result.cursor) return;
      const liveSequence = Number(result.cursor.liveSequence) || 0;
      const pointsPlayed = Number(result.cursor.pointsPlayed) || 0;
      if (needsEventRecovery(cursorRef.current, liveSequence)) {
        const recovered = await recoverEventCursor(matchId, cursorRef.current);
        cursorRef.current = Math.max(recovered, liveSequence);
      } else if (liveSequence > cursorRef.current) {
        cursorRef.current = liveSequence;
      }
      if (pointsPlayed !== pointsPlayedRef.current) {
        const have = useReplaySession.getState().points.map((point) => point.sequence);
        const missing = missingPointRange(have, pointsPlayed);
        for (const sequence of missing) {
          const point = await getPointReplay(matchId, sequence, {
            sealed: isSealedPoint(sequence, pointsPlayed),
          });
          if (point) useReplaySession.getState().appendPointReplay(point);
        }
        pointsPlayedRef.current = pointsPlayed;
      }
    } catch {
      failuresRef.current += 1;
      setConnection("reconnecting");
    } finally {
      syncingRef.current = false;
    }
  }, [matchId]);

  useEffect(() => {
    if (!enabled || !matchId) {
      setConnection("idle");
      return;
    }
    setConnection("live");
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const poll = () => {
      if (cancelled) return;
      void syncFromCursor().finally(() => {
        if (cancelled) return;
        const hidden = document.visibilityState === "hidden";
        const wait =
          failuresRef.current > 0
            ? reconnectDelayMs(failuresRef.current - 1)
            : livePollIntervalMs(hidden, clampJitter());
        timer = setTimeout(poll, wait);
      });
    };
    timer = setTimeout(poll, clampJitter());
    const onVisibility = () => {
      if (document.visibilityState === "visible") {
        if (timer) clearTimeout(timer);
        poll();
      }
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [enabled, matchId, syncFromCursor]);

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_MATCH_WS_URL;
    if (!enabled || !matchId || !url || liveTransport() !== "hybrid") return;
    let stopped = false;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let socket: WebSocket | null = null;
    let attempts = 0;

    function connect() {
      if (stopped || !url) return;
      const ws = new WebSocket(url);
      socket = ws;
      ws.addEventListener("open", () => {
        if (!stopped) setConnection("live");
        attempts = 0;
        ws.send(
          stompFrame("CONNECT", {
            "accept-version": "1.2,1.1,1.0",
            "heart-beat": "10000,10000",
          }),
        );
      });
      ws.addEventListener("message", (event) => {
        const text = typeof event.data === "string" ? event.data : "";
        if (text.startsWith("CONNECTED")) {
          ws.send(
            stompFrame("SUBSCRIBE", {
              id: `sub-${matchId}`,
              destination: `/topic/matches/${matchId}`,
            }),
          );
          void syncFromCursor();
          return;
        }
        if (!text.includes("MESSAGE")) return;
        const separator = text.indexOf("\n\n");
        const sequence = parseSequence(separator >= 0 ? text.slice(separator + 2) : "");
        if (sequence != null && sequence > cursorRef.current) {
          cursorRef.current = sequence;
        }
        void syncFromCursor();
      });
      ws.addEventListener("close", () => {
        if (stopped) return;
        setConnection("reconnecting");
        socket = null;
        attempts += 1;
        reconnectTimer = setTimeout(() => {
          void recoverEventCursor(matchId!, cursorRef.current).then((next) => {
            cursorRef.current = next;
            void syncFromCursor();
            connect();
          });
        }, reconnectDelayMs(attempts));
      });
    }

    connect();
    return () => {
      stopped = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(stompFrame("DISCONNECT", {}));
      }
      socket?.close();
    };
  }, [enabled, matchId, syncFromCursor]);

  return { connection };
}
