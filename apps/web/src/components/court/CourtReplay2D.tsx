"use client";

import { useEffect, useRef } from "react";
import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
  HALF_LENGTH_METRES,
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import { interpolateAtTime } from "@/lib/replay-space";
import { cn } from "@/lib/utils";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";
import type { Surface, Vector3 } from "@/types/replay";

type CourtReplay2DProps = {
  surface?: Surface;
  homeName: string;
  awayName: string;
  className?: string;
  label?: string;
};

type Palette = {
  out: string;
  inn: string;
  line: string;
  net: string;
};

const PALETTES: Record<Surface, Palette> = {
  GRASS: { out: "#0b5c2e", inn: "#1b7a40", line: "rgba(245,245,245,0.92)", net: "#111111" },
  CLAY: { out: "#8f3f22", inn: "#c46a3a", line: "rgba(255,248,235,0.9)", net: "#1a1a1a" },
  HARD: { out: "#1e4d8c", inn: "#3b7cc4", line: "rgba(245,245,245,0.95)", net: "#111111" },
};

const REST_HOME: Vector3 = { x: 0, y: -HALF_LENGTH_METRES + 0.85, z: 0 };
const REST_AWAY: Vector3 = { x: 0, y: HALF_LENGTH_METRES - 0.85, z: 0 };

function lastName(name: string): string {
  const parts = name.trim().split(/\s+/);
  return parts[parts.length - 1] || name;
}

function project(
  x: number,
  y: number,
  width: number,
  height: number,
): { px: number; py: number; scale: number } {
  const pad = Math.min(width, height) * 0.06;
  const scale = Math.min(
    (width - pad * 2) / (DOUBLES_HALF_WIDTH_METRES * 2),
    (height - pad * 2) / FULL_LENGTH_METRES,
  );
  return {
    px: width / 2 + x * scale,
    py: height / 2 - y * scale,
    scale,
  };
}

function strokeRect(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  halfW: number,
  halfL: number,
  lineWidth: number,
) {
  const a = project(-halfW, halfL, width, height);
  const b = project(halfW, -halfL, width, height);
  ctx.lineWidth = lineWidth;
  ctx.strokeRect(a.px, a.py, b.px - a.px, b.py - a.py);
}

function drawCourt(ctx: CanvasRenderingContext2D, width: number, height: number, palette: Palette) {
  ctx.fillStyle = palette.out;
  ctx.fillRect(0, 0, width, height);
  const outer = project(-DOUBLES_HALF_WIDTH_METRES, HALF_LENGTH_METRES, width, height);
  const outerB = project(DOUBLES_HALF_WIDTH_METRES, -HALF_LENGTH_METRES, width, height);
  ctx.fillStyle = palette.inn;
  ctx.fillRect(outer.px, outer.py, outerB.px - outer.px, outerB.py - outer.py);
  ctx.strokeStyle = palette.line;
  strokeRect(ctx, width, height, DOUBLES_HALF_WIDTH_METRES, HALF_LENGTH_METRES, 2);
  strokeRect(ctx, width, height, SINGLES_HALF_WIDTH_METRES, HALF_LENGTH_METRES, 1.4);
  const slPos = project(-SINGLES_HALF_WIDTH_METRES, SERVICE_LINE_FROM_NET_METRES, width, height);
  const slNeg = project(SINGLES_HALF_WIDTH_METRES, -SERVICE_LINE_FROM_NET_METRES, width, height);
  const slPosR = project(SINGLES_HALF_WIDTH_METRES, SERVICE_LINE_FROM_NET_METRES, width, height);
  const slNegL = project(-SINGLES_HALF_WIDTH_METRES, -SERVICE_LINE_FROM_NET_METRES, width, height);
  ctx.beginPath();
  ctx.moveTo(slPos.px, slPos.py);
  ctx.lineTo(slPosR.px, slPosR.py);
  ctx.moveTo(slNegL.px, slNegL.py);
  ctx.lineTo(slNeg.px, slNeg.py);
  ctx.moveTo(width / 2, slPos.py);
  ctx.lineTo(width / 2, slNeg.py);
  ctx.stroke();
  const netL = project(-DOUBLES_HALF_WIDTH_METRES, 0, width, height);
  const netR = project(DOUBLES_HALF_WIDTH_METRES, 0, width, height);
  ctx.strokeStyle = palette.net;
  ctx.lineWidth = 2.4;
  ctx.beginPath();
  ctx.moveTo(netL.px, netL.py);
  ctx.lineTo(netR.px, netR.py);
  ctx.stroke();
}

function drawDot(
  ctx: CanvasRenderingContext2D,
  px: number,
  py: number,
  r: number,
  fill: string,
) {
  ctx.beginPath();
  ctx.fillStyle = fill;
  ctx.arc(px, py, r, 0, Math.PI * 2);
  ctx.fill();
}

function drawLabel(
  ctx: CanvasRenderingContext2D,
  text: string,
  px: number,
  py: number,
) {
  ctx.font = "600 11px ui-sans-serif, system-ui, sans-serif";
  ctx.textAlign = "center";
  ctx.lineWidth = 3;
  ctx.strokeStyle = "rgba(0,0,0,0.55)";
  ctx.strokeText(text, px, py);
  ctx.fillStyle = "#fff";
  ctx.fillText(text, px, py);
}

/**
 * Top-down reconstructed rally. Reads the shared tape; does not own the clock.
 */
export function CourtReplay2D({
  surface = "GRASS",
  homeName,
  awayName,
  className,
  label,
}: CourtReplay2DProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const wrapRef = useRef<HTMLDivElement>(null);
  const trailRef = useRef<Vector3[]>([]);
  const lastTimeRef = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    const wrap = wrapRef.current;
    if (!canvas || !wrap) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    let raf = 0;
    let running = true;
    const palette = PALETTES[surface];

    const paint = () => {
      if (!running) return;
      raf = requestAnimationFrame(paint);
      const hidden = document.visibilityState !== "visible";
      const rect = wrap.getBoundingClientRect();
      const off =
        rect.bottom < 0 ||
        rect.right < 0 ||
        rect.top > window.innerHeight ||
        rect.left > window.innerWidth;
      if (hidden || off || rect.width < 4 || rect.height < 4) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const width = rect.width;
      const height = rect.height;
      const pixelW = Math.round(width * dpr);
      const pixelH = Math.round(height * dpr);
      if (canvas.width !== pixelW || canvas.height !== pixelH) {
        canvas.width = pixelW;
        canvas.height = pixelH;
      }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      drawCourt(ctx, width, height, palette);
      const { frames, shots } = useReplaySession.getState();
      const { timeSeconds } = usePlayback.getState();
      const pose = interpolateAtTime(frames, timeSeconds);
      const home = pose?.home ?? REST_HOME;
      const away = pose?.away ?? REST_AWAY;
      const ball = pose?.ball;
      if (timeSeconds + 1e-3 < lastTimeRef.current) trailRef.current = [];
      lastTimeRef.current = timeSeconds;
      if (ball) {
        const trail = trailRef.current;
        const prev = trail[trail.length - 1];
        if (!prev || prev.x !== ball.x || prev.y !== ball.y) {
          trail.push(ball);
          if (trail.length > 16) trail.shift();
        }
        trail.forEach((point, i) => {
          const p = project(point.x, point.y, width, height);
          drawDot(ctx, p.px, p.py, 1.6 + i * 0.12, `rgba(255,224,70,${0.12 + i / 40})`);
        });
      }
      for (const shot of shots) {
        const start = frames.find((frame) => frame.shotIndex === shot.shotIndex)?.timeSeconds ?? 0;
        if (timeSeconds < start + shot.flightSeconds * 0.55) continue;
        const land = project(shot.landing.x, shot.landing.y, width, height);
        ctx.beginPath();
        ctx.strokeStyle = "rgba(255,255,255,0.45)";
        ctx.lineWidth = 1;
        ctx.arc(land.px, land.py, 5, 0, Math.PI * 2);
        ctx.stroke();
      }
      const homeP = project(home.x, home.y, width, height);
      const awayP = project(away.x, away.y, width, height);
      drawDot(ctx, homeP.px, homeP.py, 7, "#f4f4f4");
      drawDot(ctx, awayP.px, awayP.py, 7, "#7eb6ff");
      drawLabel(ctx, lastName(homeName), homeP.px, homeP.py + 16);
      drawLabel(ctx, lastName(awayName), awayP.px, awayP.py - 10);
      if (ball) {
        const shadow = project(ball.x, ball.y, width, height);
        ctx.beginPath();
        ctx.fillStyle = "rgba(0,0,0,0.35)";
        ctx.ellipse(shadow.px, shadow.py + 4, 5, 2.2, 0, 0, Math.PI * 2);
        ctx.fill();
        const lift = Math.min(ball.z, 4) * (homeP.scale * 0.35);
        drawDot(ctx, shadow.px, shadow.py - lift, 4.2, "#ffe24a");
      }
    };
    raf = requestAnimationFrame(paint);
    return () => {
      running = false;
      cancelAnimationFrame(raf);
    };
  }, [surface, homeName, awayName]);

  return (
    <div
      ref={wrapRef}
      className={cn("relative min-h-[280px] w-full overflow-hidden bg-[#0b5c2e]", className)}
    >
      <canvas
        ref={canvasRef}
        className="absolute inset-0 h-full w-full"
        role="img"
        aria-label={label ?? `Court replay. ${homeName} versus ${awayName}.`}
      />
    </div>
  );
}
