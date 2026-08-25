export type CameraPresetId = "tv" | "baseline" | "birdsEye";

export const CAMERA_PRESET_LABELS: Record<CameraPresetId, string> = {
  tv: "TV",
  baseline: "Baseline",
  birdsEye: "Bird's eye",
};

export const DEFAULT_CAMERA_PRESET: CameraPresetId = "tv";
