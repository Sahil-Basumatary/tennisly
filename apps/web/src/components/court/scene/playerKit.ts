import { KIT_RGB, type KitSide } from "@/lib/kit-colours";
import {
  type AbstractMesh,
  type BaseTexture,
  DynamicTexture,
  PBRMaterial,
  type Scene,
} from "@babylonjs/core";

export type PlayerSide = KitSide;

type Rgb = readonly [number, number, number];

const KIT_COLOURS: Record<PlayerSide, Rgb> = KIT_RGB;

/** The mannequin's second region is its joint gaps, not skin. Tinting them
 *  flesh-toned reads as a tan-jointed doll; charcoal reads as articulation. */
const ARTICULATION_COLOUR: Rgb = [38, 40, 46];

/** Vendor albedo is a flat blue body plus darker joints — the only two regions
 *  the asset actually distinguishes, confirmed against its roughness mask. */
const KIT_MIN_BLUE = 60;
const KIT_BLUE_DOMINANCE = 1.4;

const TARGET_TEXELS = 1024;

/** Stable id stamped on the skinned mesh by vendor-assets/convert_players.py.
 *  The racket shares the same GLB and must keep its own diffuse. */
const BODY_MATERIAL = "tennisly_body";

/**
 * Repaints the mannequin's single-colour albedo into a broadcast kit. The
 * source ships one saturated blue for the whole body, so tinting at the
 * texture level is the only way to tell the two players apart on court.
 */
export async function applyKitColours(
  scene: Scene,
  meshes: AbstractMesh[],
  side: PlayerSide,
  name: string,
): Promise<void> {
  const kit = KIT_COLOURS[side];
  for (const mesh of meshes) {
    const material = mesh.material;
    if (!(material instanceof PBRMaterial)) continue;
    if (!material.name.startsWith(BODY_MATERIAL)) continue;
    const source = material.albedoTexture;
    if (!source) continue;
    const { width } = source.getSize();
    if (width <= 0) continue;
    const level = Math.max(0, Math.round(Math.log2(width / TARGET_TEXELS)));
    let pixels: ArrayBufferView | null;
    try {
      pixels = await source.readPixels(0, level);
    } catch {
      continue;
    }
    if (!pixels) continue;
    const src = new Uint8Array(pixels.buffer, pixels.byteOffset, pixels.byteLength);
    const size = Math.round(Math.sqrt(src.length / 4));
    if (size * size * 4 !== src.length) continue;

    const texture = new DynamicTexture(
      `${name}_kit`,
      { width: size, height: size },
      scene,
      true,
    );
    const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
    const image = ctx.createImageData(size, size);
    paintKit(src, image.data, kit);
    ctx.putImageData(image, 0, 0);
    texture.update(false);
    material.albedoTexture = texture;
    if (!isStillReferenced(scene, source)) {
      source.dispose();
    }
  }
}

function isStillReferenced(scene: Scene, texture: BaseTexture): boolean {
  return scene.materials.some(
    (material) => material instanceof PBRMaterial && material.albedoTexture === texture,
  );
}

function paintKit(src: Uint8Array, out: Uint8ClampedArray, kit: Rgb): void {
  for (let i = 0; i < src.length; i += 4) {
    const isKit = src[i + 2] > KIT_MIN_BLUE && src[i + 2] > src[i] * KIT_BLUE_DOMINANCE;
    const target = isKit ? kit : ARTICULATION_COLOUR;
    out[i] = target[0];
    out[i + 1] = target[1];
    out[i + 2] = target[2];
    out[i + 3] = 255;
  }
}
