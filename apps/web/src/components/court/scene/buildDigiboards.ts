import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
  HALF_LENGTH_METRES,
} from "@/lib/court-geometry";
import { APRON_MARGIN_X, APRON_MARGIN_Z } from "./buildCourt";
import {
  Color3,
  DynamicTexture,
  Mesh,
  MeshBuilder,
  type Observer,
  type Scene,
  StandardMaterial,
  Texture,
  Vector3,
  type Nullable,
} from "@babylonjs/core";

const BOARD_HEIGHT = 0.95;
const BOARD_THICKNESS = 0.05;
const SCROLL_UV_PER_SEC = 0.12;

export type DigiboardBuild = {
  root: Mesh;
  dispose: () => void;
};

/**
 * Broadcast-style LED digiboards around the apron walls: black face,
 * white TENNISLY wordmark, continuous scroll.
 */
export function buildDigiboards(scene: Scene): DigiboardBuild {
  const root = new Mesh("digiboardsRoot", scene);
  const texture = paintLedTexture(scene);
  const material = new StandardMaterial("digiboardMat", scene);
  material.diffuseTexture = texture;
  material.emissiveTexture = texture;
  material.emissiveColor = new Color3(0.55, 0.55, 0.55);
  material.specularColor = new Color3(0.02, 0.02, 0.02);
  material.backFaceCulling = false;

  const sideX = DOUBLES_HALF_WIDTH_METRES + APRON_MARGIN_X - 0.08;
  const endZ = HALF_LENGTH_METRES + APRON_MARGIN_Z - 0.08;
  const sideLength = FULL_LENGTH_METRES + APRON_MARGIN_Z * 1.6;
  const endLength = DOUBLES_HALF_WIDTH_METRES * 2 + APRON_MARGIN_X * 1.4;
  const boardY = BOARD_HEIGHT / 2 + 0.04;

  // Same placement that was visible before — only the paint is flipped upright
  placeBoard(scene, root, material, "digiboardW", sideLength, new Vector3(-sideX, boardY, 0), Math.PI / 2);
  placeBoard(scene, root, material, "digiboardE", sideLength, new Vector3(sideX, boardY, 0), -Math.PI / 2);
  placeBoard(scene, root, material, "digiboardN", endLength, new Vector3(0, boardY, endZ), Math.PI);
  placeBoard(scene, root, material, "digiboardS", endLength, new Vector3(0, boardY, -endZ), 0);

  let observer: Nullable<Observer<Scene>> = scene.onBeforeRenderObservable.add(() => {
    const dt = scene.getEngine().getDeltaTime() / 1000;
    texture.uOffset = (texture.uOffset + SCROLL_UV_PER_SEC * dt) % 1;
  });

  return {
    root,
    dispose: () => {
      if (observer) {
        scene.onBeforeRenderObservable.remove(observer);
        observer = null;
      }
    },
  };
}

function placeBoard(
  scene: Scene,
  parent: Mesh,
  material: StandardMaterial,
  name: string,
  widthMetres: number,
  position: Vector3,
  yaw: number,
): void {
  const board = MeshBuilder.CreateBox(
    name,
    { width: widthMetres, height: BOARD_HEIGHT, depth: BOARD_THICKNESS },
    scene,
  );
  board.position = position;
  board.rotation.y = yaw;
  board.material = material;
  board.parent = parent;
  board.isPickable = false;
  board.receiveShadows = false;
}

function paintLedTexture(scene: Scene): DynamicTexture {
  const w = 2048;
  const h = 128;
  const texture = new DynamicTexture("digiboardTex", { width: w, height: h }, scene, true);
  texture.wrapU = Texture.WRAP_ADDRESSMODE;
  texture.wrapV = Texture.CLAMP_ADDRESSMODE;
  // Box face UVs show the canvas inverted; 180° texture rotation corrects it
  texture.wAng = Math.PI;
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  ctx.fillStyle = "#050505";
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = "rgba(255,255,255,0.04)";
  ctx.lineWidth = 1;
  for (let x = 0; x < w; x += 8) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }
  ctx.fillStyle = "#f4f4f4";
  ctx.font = "bold 72px Helvetica, Arial, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  const word = "TENNISLY";
  const gap = 420;
  for (let x = gap / 2; x < w + gap; x += gap) {
    ctx.fillText(word, x, h / 2 + 2);
  }
  texture.update();
  return texture;
}
