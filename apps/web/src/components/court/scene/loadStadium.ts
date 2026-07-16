import type { Surface } from "@/types/replay";
import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
} from "@/lib/court-geometry";
import {
  type AbstractMesh,
  ImportMeshAsync,
  Mesh,
  type Scene,
  Vector3,
} from "@babylonjs/core";
import "@babylonjs/loaders/glTF";

const MODEL_URLS: Record<Surface, string> = {
  GRASS: "/models/stadium_grass.glb",
  // Clay reuses the indoor bowl; our clay PBR mesh replaces the model's blue court
  CLAY: "/models/stadium_hard.glb",
  HARD: "/models/stadium_hard.glb",
};

const REGULATION_RATIO = FULL_LENGTH_METRES / (DOUBLES_HALF_WIDTH_METRES * 2);
const RATIO_TOLERANCE = 0.25;
const COURT_KEYWORDS = ["court", "field", "pitch", "floor", "ground"];

export type StadiumLoadResult = {
  root: Mesh;
  courtLengthMetres: number;
  courtWidthMetres: number;
  scaleApplied: number;
  alignedTo: string | null;
};

export function hasStadiumModel(surface: Surface): boolean {
  return surface in MODEL_URLS;
}

type Footprint = {
  sizeX: number;
  sizeY: number;
  sizeZ: number;
  centre: Vector3;
  topY: number;
};

function footprintOf(mesh: AbstractMesh): Footprint {
  mesh.computeWorldMatrix(true);
  const b = mesh.getBoundingInfo().boundingBox;
  return {
    sizeX: b.maximumWorld.x - b.minimumWorld.x,
    sizeY: b.maximumWorld.y - b.minimumWorld.y,
    sizeZ: b.maximumWorld.z - b.minimumWorld.z,
    centre: b.minimumWorld.add(b.maximumWorld).scale(0.5),
    topY: b.maximumWorld.y,
  };
}

/**
 * Loads a licensed stadium GLB and aligns it onto our regulation coordinate
 * system (court centre at origin, baselines at ±11.885m along z). Alignment is
 * driven by the single mesh whose flat footprint best matches the regulation
 * doubles-court aspect ratio — found by name when the model labels it, by
 * geometry when it does not.
 */
export async function loadStadium(
  scene: Scene,
  surface: Surface,
): Promise<StadiumLoadResult | null> {
  const url = MODEL_URLS[surface];
  if (!url) return null;

  const result = await ImportMeshAsync(url, scene);
  const root = new Mesh(`stadiumRoot_${surface}`, scene);
  for (const mesh of result.meshes) {
    if (!mesh.parent) mesh.parent = root;
    mesh.isPickable = false;
  }

  const courtMesh = pickCourtMesh(result.meshes);
  if (!courtMesh) {
    return {
      root,
      courtLengthMetres: 0,
      courtWidthMetres: 0,
      scaleApplied: 1,
      alignedTo: null,
    };
  }

  const fp = footprintOf(courtMesh);
  const lengthAlongX = fp.sizeX > fp.sizeZ;
  if (lengthAlongX) {
    root.rotation = new Vector3(0, Math.PI / 2, 0);
  }
  const modelLength = lengthAlongX ? fp.sizeX : fp.sizeZ;
  const modelWidth = lengthAlongX ? fp.sizeZ : fp.sizeX;
  const scale = FULL_LENGTH_METRES / modelLength;
  root.scaling = new Vector3(scale, scale, scale);

  const cx = lengthAlongX ? -fp.centre.z : fp.centre.x;
  const cz = lengthAlongX ? fp.centre.x : fp.centre.z;
  root.position = new Vector3(-cx * scale, -fp.topY * scale, -cz * scale);

  // Run after alignment so world-space centres are relative to court origin
  disableModelNets(result.meshes);

  return {
    root,
    courtLengthMetres: modelLength * scale,
    courtWidthMetres: modelWidth * scale,
    scaleApplied: scale,
    alignedTo: courtMesh.name,
  };
}

function pickCourtMesh(meshes: AbstractMesh[]): AbstractMesh | null {
  const byName = pickByRatio(
    meshes.filter((m) => {
      const name = m.name.toLowerCase();
      return COURT_KEYWORDS.some((k) => name.includes(k));
    }),
  );
  if (byName) return byName;
  // Generic mesh names: fall back to flat meshes with a court-shaped footprint
  return pickByRatio(
    meshes.filter((m) => {
      if (m.getTotalVertices() === 0) return false;
      const fp = footprintOf(m);
      const span = Math.max(fp.sizeX, fp.sizeZ);
      return span > 0 && fp.sizeY < Math.max(0.5, span * 0.03);
    }),
  );
}

function pickByRatio(candidates: AbstractMesh[]): AbstractMesh | null {
  let best: AbstractMesh | null = null;
  let bestDelta = Infinity;
  for (const mesh of candidates) {
    if (mesh.getTotalVertices() === 0) continue;
    const fp = footprintOf(mesh);
    if (fp.sizeX <= 0 || fp.sizeZ <= 0) continue;
    const ratio = Math.max(fp.sizeX, fp.sizeZ) / Math.min(fp.sizeX, fp.sizeZ);
    const delta = Math.abs(ratio - REGULATION_RATIO);
    if (delta < bestDelta) {
      bestDelta = delta;
      best = mesh;
    }
  }
  return bestDelta <= RATIO_TOLERANCE ? best : null;
}

/**
 * Hides every model-owned net (and leftover cord/tape strips) after the court
 * has been centred at the origin. Indoor assets ship multiple net groups; some
 * remnants are only a few centimetres tall and fail a full-net height check.
 */
function disableModelNets(meshes: AbstractMesh[]): void {
  const halfWidth = DOUBLES_HALF_WIDTH_METRES;
  const halfLength = FULL_LENGTH_METRES / 2;
  for (const mesh of meshes) {
    if (mesh.name.toLowerCase().includes("net")) {
      mesh.setEnabled(false);
      continue;
    }
    if (mesh.getTotalVertices() === 0) continue;
    const fp = footprintOf(mesh);
    const spansCourt =
      fp.sizeX >= halfWidth * 1.4 && fp.sizeX <= halfWidth * 3.6;
    const thinAlongDepth = fp.sizeZ < 1.5;
    // Full net (~0.9m) or a leftover cord/tape (~2cm+)
    const netLikeHeight = fp.sizeY > 0.02 && fp.sizeY < 2.5;
    const nearMidcourt = Math.abs(fp.centre.z) < halfLength * 0.45;
    const nearPlayingSurface = fp.centre.y > -0.2 && fp.centre.y < 2.5;
    if (spansCourt && thinAlongDepth && netLikeHeight && nearMidcourt && nearPlayingSurface) {
      mesh.setEnabled(false);
    }
  }
}
