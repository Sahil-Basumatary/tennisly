"use client";

import { useEffect, useRef } from "react";

type MatchLiveBridgeProps = {
  matchId: string;
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

async function replayAfter(matchId: string, afterSequence: number): Promise<number> {
  let cursor = afterSequence;
  for (let page = 0; page < 32; page += 1) {
    const response = await fetch(
      `/api/matches/${matchId}/events?afterSequence=${cursor}&limit=1000`,
      { cache: "no-store" },
    );
    if (!response.ok) {
      return cursor;
    }
    const events: Array<{ sequence?: number }> = await response.json();
    if (!Array.isArray(events) || events.length === 0) {
      return cursor;
    }
    for (const event of events) {
      const sequence = Number(event.sequence);
      if (Number.isSafeInteger(sequence) && sequence > cursor) {
        cursor = sequence;
      }
    }
    if (events.length < 1000) {
      return cursor;
    }
  }
  return cursor;
}

export function MatchLiveBridge({ matchId }: MatchLiveBridgeProps) {
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_MATCH_WS_URL;
    if (!url || !matchId) {
      return;
    }
    let stopped = false;
    let cursor = 0;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;

    function connect() {
      if (stopped || !url) {
        return;
      }
      const ws = new WebSocket(url);
      socketRef.current = ws;
      ws.addEventListener("open", () => {
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
          if (cursor > 0) {
            void replayAfter(matchId, cursor).then((next) => {
              if (!stopped && next > cursor) {
                cursor = next;
              }
            });
          }
          return;
        }
        if (!text.includes("MESSAGE")) {
          return;
        }
        const separator = text.indexOf("\n\n");
        const sequence = parseSequence(separator >= 0 ? text.slice(separator + 2) : "");
        if (sequence != null && sequence > cursor) {
          cursor = sequence;
        }
      });
      ws.addEventListener("close", () => {
        if (stopped) {
          return;
        }
        socketRef.current = null;
        reconnectTimer = setTimeout(() => {
          void replayAfter(matchId, cursor).then((next) => {
            cursor = next;
            connect();
          });
        }, 400);
      });
    }

    connect();
    return () => {
      stopped = true;
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      const ws = socketRef.current;
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(stompFrame("DISCONNECT", {}));
      }
      ws?.close();
    };
  }, [matchId]);

  return null;
}
