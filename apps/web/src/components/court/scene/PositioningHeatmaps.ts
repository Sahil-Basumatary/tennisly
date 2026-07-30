import { FULL_LENGTH_METRES } from "@/lib/court-geometry";
import { buildHeatGrid, type HeatGrid, type HeatSide } from "@/lib/heat-grid";
import type { ReplayFrame } from "@/types/replay";
import {
  Color3,
  DynamicTexture,
  Mesh,
  MeshBuilder,
  type Scene,
  StandardMaterial,
  Texture,
} from "@babylonjs/core";

export type HeatmapVisibility = {
  heatmapHome: boolean;
  heatmapAway: boolean;
};

/**
 * Court-plane positioning heatmaps (gaussian splat density from replay frames).
 */
export class PositioningHeatmaps {
  readonly root: Mesh;
  private readonly homePlane: Mesh;
  private readonly awayPlane: Mesh;
  private visibility: HeatmapVisibility = {
    heatmapHome: false,
    heatmapAway: false,
  };

  constructor(scene: Scene, frames: ReplayFrame[]) {
    this.root = new Mesh("positionHeatmaps", scene);
    const homeGrid = buildHeatGrid(frames, "home");
    const awayGrid = buildHeatGrid(frames, "away");
    this.homePlane = buildHeatPlane(scene, this.root, "home", homeGrid, {
      r: 1,
      g: 0.45,
      b: 0.08,
    });
    this.awayPlane = buildHeatPlane(scene, this.root, "away", awayGrid, {
      r: 0.15,
      g: 0.55,
      b: 1,
    });
    this.applyVisibility();
  }

  setVisibility(flags: HeatmapVisibility): void {
    this.visibility = flags;
    this.applyVisibility();
  }

  private applyVisibility(): void {
    this.homePlane.setEnabled(this.visibility.heatmapHome);
    this.awayPlane.setEnabled(this.visibility.heatmapAway);
  }

  dispose(): void {
    this.root.dispose();
  }
}

function buildHeatPlane(
  scene: Scene,
  parent: Mesh,
  side: HeatSide,
  grid: HeatGrid,
  tint: { r: number; g: number; b: number },
): Mesh {
  const tex = paintHeatTexture(scene, side, grid, tint);
  const plane = MeshBuilder.CreateGround(
    `heatmap_${side}`,
    {
      width: grid.halfWidthMetres * 2,
      height: FULL_LENGTH_METRES,
    },
    scene,
  );
  plane.position.y = side === "home" ? 0.035 : 0.038;
  const mat = new StandardMaterial(`heatmapMat_${side}`, scene);
  mat.diffuseTexture = tex;
  mat.emissiveTexture = tex;
  mat.opacityTexture = tex;
  mat.useAlphaFromDiffuseTexture = true;
  mat.emissiveColor = new Color3(0.35, 0.35, 0.35);
  mat.specularColor = new Color3(0, 0, 0);
  mat.backFaceCulling = false;
  mat.disableLighting = false;
  plane.material = mat;
  plane.parent = parent;
  plane.isPickable = false;
  plane.setEnabled(false);
  return plane;
}

function paintHeatTexture(
  scene: Scene,
  side: HeatSide,
  grid: HeatGrid,
  tint: { r: number; g: number; b: number },
): DynamicTexture {
  const w = grid.width;
  const h = grid.height;
  const texture = new DynamicTexture(
    `heatmapTex_${side}`,
    { width: w, height: h },
    scene,
    false,
  );
  texture.wrapU = Texture.CLAMP_ADDRESSMODE;
  texture.wrapV = Texture.CLAMP_ADDRESSMODE;
  texture.hasAlpha = true;
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  const img = ctx.createImageData(w, h);
  const data = img.data;
  for (let z = 0; z < h; z++) {
    for (let x = 0; x < w; x++) {
      // Canvas v=0 is top; court +z (replay +y) should be top of texture for CreateGround
      const density = grid.cells[(h - 1 - z) * w + x];
      const i = (z * w + x) * 4;
      if (density < 0.04) {
        data[i] = 0;
        data[i + 1] = 0;
        data[i + 2] = 0;
        data[i + 3] = 0;
        continue;
      }
      const a = Math.min(1, density * 0.85);
      data[i] = Math.round(tint.r * 255);
      data[i + 1] = Math.round(tint.g * 255);
      data[i + 2] = Math.round(tint.b * 255);
      data[i + 3] = Math.round(a * 255);
    }
  }
  ctx.putImageData(img, 0, 0);
  texture.update();
  return texture;
}
