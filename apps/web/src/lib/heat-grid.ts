import type { ReplayFrame } from "@/types/replay";
import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
  HALF_LENGTH_METRES,
} from "@/lib/court-geometry";

export type HeatSide = "home" | "away";

export type HeatGrid = {
  width: number;
  height: number;
  /** Row-major density [0..1] after normalize. */
  cells: Float32Array;
  halfWidthMetres: number;
  halfLengthMetres: number;
};

const DEFAULT_RES_X = 64;
const DEFAULT_RES_Z = 96;
const SIGMA_METRES = 1.15;
const FRAME_STRIDE = 2;

/**
 * Accumulate gaussian splats of player feet positions into a court-aligned grid.
 * Pure — safe to unit-test without Babylon.
 */
export function buildHeatGrid(
  frames: ReplayFrame[],
  side: HeatSide,
  options?: { resX?: number; resZ?: number; sigmaMetres?: number },
): HeatGrid {
  const resX = options?.resX ?? DEFAULT_RES_X;
  const resZ = options?.resZ ?? DEFAULT_RES_Z;
  const sigma = options?.sigmaMetres ?? SIGMA_METRES;
  const halfW = DOUBLES_HALF_WIDTH_METRES;
  const halfL = HALF_LENGTH_METRES;
  const cells = new Float32Array(resX * resZ);
  const invTwoSigma2 = 1 / (2 * sigma * sigma);
  const radiusCells = Math.ceil((sigma * 3 * resX) / (halfW * 2));

  for (let i = 0; i < frames.length; i += FRAME_STRIDE) {
    const pos = side === "home" ? frames[i].home : frames[i].away;
    const ux = (pos.x + halfW) / (halfW * 2);
    const uz = (pos.y + halfL) / FULL_LENGTH_METRES;
    if (ux < -0.05 || ux > 1.05 || uz < -0.05 || uz > 1.05) continue;
    const cx = Math.floor(ux * (resX - 1));
    const cz = Math.floor(uz * (resZ - 1));
    for (let dz = -radiusCells; dz <= radiusCells; dz++) {
      for (let dx = -radiusCells; dx <= radiusCells; dx++) {
        const x = cx + dx;
        const z = cz + dz;
        if (x < 0 || x >= resX || z < 0 || z >= resZ) continue;
        const wx = ((x + 0.5) / resX) * halfW * 2 - halfW;
        const wz = ((z + 0.5) / resZ) * FULL_LENGTH_METRES - halfL;
        const d2 = (wx - pos.x) ** 2 + (wz - pos.y) ** 2;
        cells[z * resX + x] += Math.exp(-d2 * invTwoSigma2);
      }
    }
  }

  let max = 0;
  for (let i = 0; i < cells.length; i++) max = Math.max(max, cells[i]);
  if (max > 0) {
    for (let i = 0; i < cells.length; i++) cells[i] /= max;
  }

  return { width: resX, height: resZ, cells, halfWidthMetres: halfW, halfLengthMetres: halfL };
}
