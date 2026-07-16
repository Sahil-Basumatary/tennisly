import type { Surface } from "@/types/replay";
import {
  Color3,
  PBRMaterial,
  type Scene,
  StandardMaterial,
  Texture,
} from "@babylonjs/core";

export type SurfacePalette = {
  inBounds: Color3;
  apron: Color3;
  line: Color3;
};

/** Broadcast-calibrated tints multiplied over the photo textures. */
const PALETTES: Record<Surface, SurfacePalette> = {
  GRASS: {
    inBounds: new Color3(0.42, 0.52, 0.32),
    apron: new Color3(0.3, 0.4, 0.24),
    line: new Color3(0.93, 0.93, 0.9),
  },
  CLAY: {
    inBounds: new Color3(0.78, 0.42, 0.26),
    apron: new Color3(0.62, 0.32, 0.2),
    line: new Color3(0.95, 0.95, 0.93),
  },
  HARD: {
    inBounds: new Color3(0.16, 0.32, 0.58),
    apron: new Color3(0.1, 0.42, 0.34),
    line: new Color3(0.95, 0.95, 0.94),
  },
};

const TEXTURE_BASE = "/textures/court";

const TEXTURE_PREFIX: Record<Surface, string> = {
  GRASS: "grass",
  CLAY: "clay",
  HARD: "hard",
};

/** World-space metres covered by one texture tile, per surface. */
const TILE_METRES: Record<Surface, number> = {
  GRASS: 4,
  CLAY: 3,
  HARD: 6,
};

export function surfacePalette(surface: Surface): SurfacePalette {
  return PALETTES[surface];
}

function loadMap(scene: Scene, prefix: string, kind: string): Texture {
  const tex = new Texture(`${TEXTURE_BASE}/${prefix}_${kind}.jpg`, scene);
  tex.wrapU = Texture.WRAP_ADDRESSMODE;
  tex.wrapV = Texture.WRAP_ADDRESSMODE;
  return tex;
}

function tile(tex: Texture, sizeMetres: { u: number; v: number }, surface: Surface): void {
  tex.uScale = sizeMetres.u / TILE_METRES[surface];
  tex.vScale = sizeMetres.v / TILE_METRES[surface];
}

export function createSurfaceMaterial(
  scene: Scene,
  surface: Surface,
  variant: "court" | "apron",
  sizeMetres: { u: number; v: number },
): PBRMaterial {
  const palette = PALETTES[surface];
  const prefix = TEXTURE_PREFIX[surface];
  const mat = new PBRMaterial(`${variant}_${surface}`, scene);
  mat.metallic = 0;
  mat.roughness = 1;

  const albedo = loadMap(scene, prefix, "color");
  tile(albedo, sizeMetres, surface);
  mat.albedoTexture = albedo;
  mat.albedoColor = variant === "court" ? palette.inBounds : palette.apron;

  const bump = loadMap(scene, prefix, "normal");
  tile(bump, sizeMetres, surface);
  mat.bumpTexture = bump;
  mat.bumpTexture.level = surface === "HARD" ? 0.35 : 0.8;

  const rough = loadMap(scene, prefix, "roughness");
  tile(rough, sizeMetres, surface);
  mat.metallicTexture = rough;
  mat.useRoughnessFromMetallicTextureGreen = true;
  mat.useRoughnessFromMetallicTextureAlpha = false;
  mat.useMetallnessFromMetallicTextureBlue = false;

  mat.environmentIntensity = 1.0;
  mat.specularIntensity = surface === "HARD" ? 0.7 : 0.35;
  return mat;
}

export function createLineMaterial(scene: Scene, surface: Surface): StandardMaterial {
  const mat = new StandardMaterial(`line_${surface}`, scene);
  mat.diffuseColor = PALETTES[surface].line;
  mat.emissiveColor = PALETTES[surface].line.scale(0.06);
  mat.specularColor = new Color3(0.12, 0.12, 0.12);
  return mat;
}
