import type { Surface } from "@/types/replay";
import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
  HALF_LENGTH_METRES,
  NET_HEIGHT_CENTRE_METRES,
  NET_HEIGHT_POST_METRES,
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import {
  Color3,
  DynamicTexture,
  Mesh,
  MeshBuilder,
  PBRMaterial,
  type Scene,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";
import { createLineMaterial, createSurfaceMaterial } from "./surfaceMaterials";

const LINE_WIDTH = 0.05;
const LINE_Y = 0.01;
const BASELINE_WIDTH = 0.1;
export const APRON_MARGIN_X = 3.66;
export const APRON_MARGIN_Z = 6.4;

export type CourtBuild = {
  root: Mesh;
  shadowReceivers: Mesh[];
  shadowCasters: Mesh[];
};

export function buildCourt(scene: Scene, surface: Surface): CourtBuild {
  const root = new Mesh("courtRoot", scene);
  const shadowReceivers: Mesh[] = [];
  const shadowCasters: Mesh[] = [];

  const apronW = DOUBLES_HALF_WIDTH_METRES * 2 + APRON_MARGIN_X * 2;
  const apronD = FULL_LENGTH_METRES + APRON_MARGIN_Z * 2;
  const apron = MeshBuilder.CreateGround("apron", { width: apronW, height: apronD }, scene);
  apron.position.y = -0.003;
  apron.material = createSurfaceMaterial(scene, surface, "apron", { u: apronW, v: apronD });
  apron.parent = root;
  apron.receiveShadows = true;
  shadowReceivers.push(apron);

  const courtW = DOUBLES_HALF_WIDTH_METRES * 2;
  const court = MeshBuilder.CreateGround(
    "courtInBounds",
    { width: courtW, height: FULL_LENGTH_METRES },
    scene,
  );
  court.material = createSurfaceMaterial(scene, surface, "court", {
    u: courtW,
    v: FULL_LENGTH_METRES,
  });
  court.parent = root;
  court.receiveShadows = true;
  shadowReceivers.push(court);

  const lines = buildLines(scene, createLineMaterial(scene, surface));
  lines.parent = root;

  const net = buildNet(scene);
  net.parent = root;
  for (const mesh of net.getChildMeshes(false)) {
    shadowCasters.push(mesh as Mesh);
  }

  return { root, shadowReceivers, shadowCasters };
}

function buildLines(scene: Scene, material: StandardMaterial): Mesh {
  const root = new Mesh("linesRoot", scene);
  const sw = SINGLES_HALF_WIDTH_METRES;
  const dw = DOUBLES_HALF_WIDTH_METRES;
  const hl = HALF_LENGTH_METRES;
  const sl = SERVICE_LINE_FROM_NET_METRES;

  addLine(scene, root, material, "baselineN", dw * 2, BASELINE_WIDTH, 0, hl);
  addLine(scene, root, material, "baselineS", dw * 2, BASELINE_WIDTH, 0, -hl);
  addLine(scene, root, material, "doublesE", LINE_WIDTH, FULL_LENGTH_METRES, dw, 0);
  addLine(scene, root, material, "doublesW", LINE_WIDTH, FULL_LENGTH_METRES, -dw, 0);
  addLine(scene, root, material, "singlesE", LINE_WIDTH, FULL_LENGTH_METRES, sw, 0);
  addLine(scene, root, material, "singlesW", LINE_WIDTH, FULL_LENGTH_METRES, -sw, 0);
  addLine(scene, root, material, "serviceN", sw * 2, LINE_WIDTH, 0, sl);
  addLine(scene, root, material, "serviceS", sw * 2, LINE_WIDTH, 0, -sl);
  addLine(scene, root, material, "centreService", LINE_WIDTH, sl * 2, 0, 0);
  addLine(scene, root, material, "centreMarkN", 0.1, LINE_WIDTH, 0, hl - 0.1);
  addLine(scene, root, material, "centreMarkS", 0.1, LINE_WIDTH, 0, -hl + 0.1);
  return root;
}

function addLine(
  scene: Scene,
  parent: Mesh,
  material: StandardMaterial,
  name: string,
  widthX: number,
  depthZ: number,
  x: number,
  z: number,
): void {
  const mesh = MeshBuilder.CreateGround(name, { width: widthX, height: depthZ }, scene);
  mesh.position = new Vector3(x, LINE_Y, z);
  mesh.material = material;
  mesh.parent = parent;
  mesh.isPickable = false;
}

function buildNet(scene: Scene): Mesh {
  const root = new Mesh("netRoot", scene);
  const halfW = DOUBLES_HALF_WIDTH_METRES + 0.15;

  const postMat = new PBRMaterial("netPostMat", scene);
  postMat.albedoColor = new Color3(0.05, 0.06, 0.07);
  postMat.metallic = 0.6;
  postMat.roughness = 0.4;

  for (const side of [-1, 1] as const) {
    const post = MeshBuilder.CreateCylinder(
      side < 0 ? "netPostW" : "netPostE",
      { height: NET_HEIGHT_POST_METRES, diameter: 0.08, tessellation: 16 },
      scene,
    );
    post.position = new Vector3(side * halfW, NET_HEIGHT_POST_METRES / 2, 0);
    post.material = postMat;
    post.parent = root;
  }

  const netMesh = MeshBuilder.CreatePlane(
    "netMesh",
    { width: halfW * 2, height: NET_HEIGHT_CENTRE_METRES },
    scene,
  );
  netMesh.position = new Vector3(0, NET_HEIGHT_CENTRE_METRES / 2, 0);
  const netMat = new StandardMaterial("netMat", scene);
  const netTex = new DynamicTexture("netTex", { width: 1024, height: 128 }, scene, true);
  paintNetTexture(netTex);
  netMat.diffuseTexture = netTex;
  netMat.opacityTexture = netTex;
  netMat.useAlphaFromDiffuseTexture = true;
  netMat.backFaceCulling = false;
  netMat.specularColor = new Color3(0.05, 0.05, 0.05);
  netMesh.material = netMat;
  netMesh.parent = root;

  const tape = MeshBuilder.CreateBox(
    "netTape",
    { width: halfW * 2, height: 0.06, depth: 0.015 },
    scene,
  );
  tape.position = new Vector3(0, NET_HEIGHT_CENTRE_METRES + 0.015, 0);
  const tapeMat = new PBRMaterial("netTapeMat", scene);
  tapeMat.albedoColor = new Color3(0.92, 0.92, 0.9);
  tapeMat.metallic = 0;
  tapeMat.roughness = 0.6;
  tape.material = tapeMat;
  tape.parent = root;

  return root;
}

function paintNetTexture(texture: DynamicTexture): void {
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  const w = texture.getSize().width;
  const h = texture.getSize().height;
  ctx.clearRect(0, 0, w, h);
  ctx.strokeStyle = "rgba(30,30,32,0.85)";
  ctx.lineWidth = 1.5;
  const step = 6;
  for (let x = 0; x <= w; x += step) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }
  for (let y = 0; y <= h; y += step) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }
  texture.hasAlpha = true;
  texture.update();
}
