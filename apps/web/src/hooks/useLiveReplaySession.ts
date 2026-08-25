"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { recoverEventCursor, unseenPointSequences } from "@/lib/live-replay-sync";
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

async function fetchLedger(matchId: string): Promise<Array<{ sequence?: unknown; sequenceNumber?: unknown }>> {
  const response = await fetch(`/api/matches/${matchId}/points`, { cache: "no-store" });
  if (!response.ok) return [];
  const body: unknown = await response.json();
  return Array.isArray(body) ? body : [];
}

/**
 * WS is a wake-up; the point ledger is the source of truth. Polling covers
 * brokers that drop silently. Appends are ordered and idempotent.
 */
export function useLiveReplaySession({
  matchId,
  enabled = false,
}: UseLiveReplaySessionOptions): { connection: LiveConnection } {
  const [connection, setConnection] = useState<LiveConnection>("idle");
  const syncingRef = useRef(false);
  const cursorRef = useRef(0);

  const syncPoints = useCallback(async () => {
    if (!matchId || syncingRef.current) return;
    syncingRef.current = true;
    try {
      const ledger = await fetchLedger(matchId);
      const have = useReplaySession.getState().points.map((point) => point.sequence);
      const missing = unseenPointSequences(have, ledger);
      for (const sequence of missing) {
        const point = await getPointReplay(matchId, sequence);
        if (point) useReplaySession.getState().appendPointReplay(point);
      }
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
    void syncPoints();
    const poll = window.setInterval(() => {
      void syncPoints();
    }, 4000);
    return () => window.clearInterval(poll);
  }, [enabled, matchId, syncPoints]);

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_MATCH_WS_URL;
    if (!enabled || !matchId || !url) return;
    let stopped = false;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let socket: WebSocket | null = null;

    function connect() {
      if (stopped || !url) return;
      const ws = new WebSocket(url);
      socket = ws;
      ws.addEventListener("open", () => {
        if (!stopped) setConnection("live");
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
          if (cursorRef.current > 0) {
            void recoverEventCursor(matchId!, cursorRef.current).then((next) => {
              cursorRef.current = next;
              void syncPoints();
            });
          }
          return;
        }
        if (!text.includes("MESSAGE")) return;
        const separator = text.indexOf("\n\n");
        const sequence = parseSequence(separator >= 0 ? text.slice(separator + 2) : "");
        if (sequence != null && sequence > cursorRef.current) {
          cursorRef.current = sequence;
        }
        void syncPoints();
      });
      ws.addEventListener("close", () => {
        if (stopped) return;
        setConnection("reconnecting");
        socket = null;
        reconnectTimer = setTimeout(() => {
          void recoverEventCursor(matchId!, cursorRef.current).then((next) => {
            cursorRef.current = next;
            void syncPoints();
            connect();
          });
        }, 400);
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
  }, [enabled, matchId, syncPoints]);

  return { connection };
}
