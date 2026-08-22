"use client";

import { useEffect, useRef } from "react";

type MatchLiveBridgeProps = {
  matchId: string;
};

function stompFrame(command: string, headers: Record<string, string>, body = ""): string {
  const lines = [command, ...Object.entries(headers).map(([k, v]) => `${k}:${v}`), "", body];
  return `${lines.join("\n")}\0`;
}

export function MatchLiveBridge({ matchId }: MatchLiveBridgeProps) {
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_MATCH_WS_URL;
    if (!url || !matchId) {
      return;
    }
    let stopped = false;
    const ws = new WebSocket(url);
    socketRef.current = ws;
    ws.addEventListener("open", () => {
      ws.send(stompFrame("CONNECT", { "accept-version": "1.2,1.1,1.0", "heart-beat": "10000,10000" }));
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
        return;
      }
      if (text.startsWith("MESSAGE") && process.env.NODE_ENV === "development") {
        console.debug("[match-live] delta", matchId);
      }
    });
    ws.addEventListener("close", () => {
      if (!stopped) socketRef.current = null;
    });
    return () => {
      stopped = true;
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(stompFrame("DISCONNECT", {}));
      }
      ws.close();
    };
  }, [matchId]);

  return null;
}
