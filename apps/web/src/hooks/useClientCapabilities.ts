"use client";

import { useSyncExternalStore } from "react";
import { isWebGLAvailable, prefersReducedMotion } from "@/lib/webgl";

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";

let webglSupport: boolean | null = null;

function subscribeNever(): () => void {
  return () => {};
}

function getWebGLSnapshot(): boolean {
  // Probed once per session — creating throwaway contexts is not free and the
  // answer cannot change without a reload.
  if (webglSupport === null) webglSupport = isWebGLAvailable();
  return webglSupport;
}

/**
 * `null` until hydration completes, so callers can render a neutral placeholder
 * instead of flashing the 2D fallback on the server-rendered pass.
 */
export function useWebGLSupport(): boolean | null {
  return useSyncExternalStore(subscribeNever, getWebGLSnapshot, () => null);
}

function subscribeReducedMotion(onChange: () => void): () => void {
  const media = window.matchMedia(REDUCED_MOTION_QUERY);
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

/** Tracks the OS motion preference live — users toggle it mid-session. */
export function useReducedMotion(): boolean {
  return useSyncExternalStore(subscribeReducedMotion, prefersReducedMotion, () => false);
}
