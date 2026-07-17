import { HALF_LENGTH_METRES } from "@/lib/court-geometry";
import { Vector3 } from "@babylonjs/core";

export type CameraPresetId = "tv" | "baseline" | "birdsEye";

export type CameraPose = {
  alpha: number;
  beta: number;
  radius: number;
  target: Vector3;
};

/** Broadcast-style framing — court is the hero, stands as a rim. */
export const CAMERA_PRESETS: Record<CameraPresetId, CameraPose> = {
  tv: {
    alpha: Math.PI / 2,
    beta: 1.1,
    radius: HALF_LENGTH_METRES * 2.2,
    target: new Vector3(0, 0.2, 0),
  },
  baseline: {
    alpha: Math.PI / 2,
    beta: 1.28,
    radius: HALF_LENGTH_METRES * 1.65,
    target: new Vector3(0, 0.9, 2),
  },
  birdsEye: {
    alpha: Math.PI / 2,
    beta: 0.28,
    radius: HALF_LENGTH_METRES * 3.1,
    target: new Vector3(0, 0, 0),
  },
};

export const CAMERA_PRESET_LABELS: Record<CameraPresetId, string> = {
  tv: "TV",
  baseline: "Baseline",
  birdsEye: "Bird's eye",
};

export const DEFAULT_CAMERA_PRESET: CameraPresetId = "tv";
