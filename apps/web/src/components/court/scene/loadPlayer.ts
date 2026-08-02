import {
  type AbstractMesh,
  type AnimationGroup,
  ImportMeshAsync,
  type Scene,
  Vector3,
} from "@babylonjs/core";
import "@babylonjs/loaders/glTF";
import { applyKitColours, type PlayerSide } from "./playerKit";

export type PlayerClip =
  | "idle"
  | "jog"
  | "serve"
  | "forehand"
  | "backhand"
  | "smash";

export type LoadedPlayer = {
  root: AbstractMesh;
  clips: Partial<Record<PlayerClip, AnimationGroup>>;
  heightMetres: number;
  /** Added to root.y so soles sit on the court plane. */
  footOffsetY: number;
};

const MODEL_URLS = {
  male: "/models/players/player_male.glb",
  female: "/models/players/player_female.glb",
} as const;

export type PlayerGender = keyof typeof MODEL_URLS;

const TARGET_HEIGHT_METRES: Record<PlayerGender, number> = {
  male: 1.85,
  female: 1.72,
};

/** Extra lift past AABB soles — ready-stance clips plant lower than T-pose bounds. */
const FOOT_CLEARANCE_METRES = 0.07;

/**
 * Loads a Motion Cast tennis athlete GLB (idle/jog/serve/forehand/backhand/smash).
 * Scales to tour-typical height and plants soles on the court plane.
 */
export async function loadPlayer(
  scene: Scene,
  gender: PlayerGender,
  name: string,
  side: PlayerSide,
): Promise<LoadedPlayer> {
  const result = await ImportMeshAsync(MODEL_URLS[gender], scene);
  const root =
    result.meshes.find((m) => !m.parent) ??
    result.meshes[0] ??
    (() => {
      throw new Error(`Player model missing root: ${gender}`);
    })();
  root.name = name;
  for (const mesh of result.meshes) {
    mesh.isPickable = false;
  }

  await applyKitColours(scene, result.meshes, side, name);

  const bounds = measureBounds(result.meshes);
  const targetHeight = TARGET_HEIGHT_METRES[gender];
  const scale = bounds.height > 0.01 ? targetHeight / bounds.height : 1;
  root.scaling = new Vector3(scale, scale, scale);
  const scaled = measureBounds(result.meshes);
  const footOffsetY = -scaled.minY + FOOT_CLEARANCE_METRES;

  const clips: Partial<Record<PlayerClip, AnimationGroup>> = {};
  for (const group of result.animationGroups) {
    const key = normalizeClipName(group.name);
    if (key) {
      group.stop();
      group.loopAnimation = key === "idle" || key === "jog";
      clips[key] = group;
    }
  }

  return { root, clips, heightMetres: targetHeight, footOffsetY };
}

function normalizeClipName(name: string): PlayerClip | null {
  const n = name.toLowerCase();
  if (n.includes("idle")) return "idle";
  if (n.includes("jog") || n.includes("moving") || n.includes("run")) return "jog";
  if (n.includes("serve") || n.includes("service")) return "serve";
  if (n.includes("backhand")) return "backhand";
  if (n.includes("smash")) return "smash";
  if (n.includes("forehand") || n.includes("hit")) return "forehand";
  return null;
}

function measureBounds(meshes: AbstractMesh[]): { height: number; minY: number; maxY: number } {
  let minY = Infinity;
  let maxY = -Infinity;
  for (const mesh of meshes) {
    if (mesh.getTotalVertices() === 0) continue;
    mesh.computeWorldMatrix(true);
    const b = mesh.getBoundingInfo().boundingBox;
    minY = Math.min(minY, b.minimumWorld.y);
    maxY = Math.max(maxY, b.maximumWorld.y);
  }
  if (!Number.isFinite(minY)) return { height: 0, minY: 0, maxY: 0 };
  return { height: maxY - minY, minY, maxY };
}
