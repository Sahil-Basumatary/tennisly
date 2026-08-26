export const LIVE_VISIBLE_MS = 3_000;
export const LIVE_HIDDEN_MS = 12_000;
export const LIVE_JITTER_MS = 400;
export const RECONNECT_BASE_MS = 400;
export const RECONNECT_MAX_MS = 5_000;
export const MISSING_POINT_FETCH_CAP = 16;

export type LiveTransport = "http" | "hybrid";

export function liveTransport(value = process.env.NEXT_PUBLIC_LIVE_TRANSPORT): LiveTransport {
  return value === "hybrid" ? "hybrid" : "http";
}

export function livePollIntervalMs(hidden: boolean, jitter = 0): number {
  const base = hidden ? LIVE_HIDDEN_MS : LIVE_VISIBLE_MS;
  return base + Math.max(0, jitter);
}

export function reconnectDelayMs(failures: number): number {
  const exp = Math.max(0, failures);
  return Math.min(RECONNECT_MAX_MS, RECONNECT_BASE_MS * 2 ** exp);
}

export function clampJitter(random = Math.random(), max = LIVE_JITTER_MS): number {
  const unit = Math.max(0, Math.min(1, random));
  return Math.min(max, Math.floor(unit * (max + 1)));
}
