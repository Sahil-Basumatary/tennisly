import {
  DOUBLES_HALF_WIDTH_METRES,
  FULL_LENGTH_METRES,
  HALF_LENGTH_METRES,
} from "@/lib/court-geometry";
import {
  Color3,
  DynamicTexture,
  Mesh,
  MeshBuilder,
  type Scene,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";

export function buildEnvironment(scene: Scene): Mesh {
  const root = new Mesh("envRoot", scene);
  buildStands(scene, root);
  buildHoardings(scene, root);
  buildSkyDome(scene, root);
  return root;
}

function buildStands(scene: Scene, parent: Mesh): void {
  const depth = 8;
  const height = 7;
  const z = HALF_LENGTH_METRES + 3.5 + depth / 2;
  const x = DOUBLES_HALF_WIDTH_METRES + 3.5 + depth / 2;
  const mats = standMaterial(scene);

  const north = MeshBuilder.CreateBox(
    "standN",
    { width: DOUBLES_HALF_WIDTH_METRES * 2 + 10, height, depth },
    scene,
  );
  north.position = new Vector3(0, height / 2 - 0.2, z);
  north.material = mats;
  north.parent = parent;
  north.isPickable = false;

  const south = north.clone("standS");
  south.position = new Vector3(0, height / 2 - 0.2, -z);
  south.parent = parent;

  const east = MeshBuilder.CreateBox(
    "standE",
    { width: depth, height, depth: FULL_LENGTH_METRES + 8 },
    scene,
  );
  east.position = new Vector3(x, height / 2 - 0.2, 0);
  east.material = mats;
  east.parent = parent;
  east.isPickable = false;

  const west = east.clone("standW");
  west.position = new Vector3(-x, height / 2 - 0.2, 0);
  west.parent = parent;

  const crowdN = MeshBuilder.CreatePlane(
    "crowdN",
    { width: DOUBLES_HALF_WIDTH_METRES * 2 + 8, height: height - 1.2 },
    scene,
  );
  crowdN.position = new Vector3(0, height / 2, z - depth / 2 - 0.05);
  crowdN.rotation.y = Math.PI;
  const crowdMat = crowdMaterial(scene);
  crowdN.material = crowdMat;
  crowdN.parent = parent;
  crowdN.isPickable = false;

  const crowdS = crowdN.clone("crowdS");
  crowdS.position = new Vector3(0, height / 2, -(z - depth / 2 - 0.05));
  crowdS.rotation.y = 0;
  crowdS.parent = parent;
}

function standMaterial(scene: Scene): StandardMaterial {
  const mat = new StandardMaterial("standMat", scene);
  mat.diffuseColor = new Color3(0.22, 0.24, 0.26);
  mat.emissiveColor = new Color3(0.04, 0.045, 0.05);
  mat.specularColor = Color3.Black();
  return mat;
}

function crowdMaterial(scene: Scene): StandardMaterial {
  const mat = new StandardMaterial("crowdMat", scene);
  const tex = new DynamicTexture("crowdTex", { width: 1024, height: 256 }, scene, false);
  paintCrowd(tex);
  mat.diffuseTexture = tex;
  mat.emissiveColor = new Color3(0.06, 0.06, 0.07);
  mat.specularColor = Color3.Black();
  return mat;
}

function buildHoardings(scene: Scene, parent: Mesh): void {
  const z = HALF_LENGTH_METRES + 2.2;
  const x = DOUBLES_HALF_WIDTH_METRES + 2.4;
  const boards: { name: string; w: number; h: number; pos: Vector3; rotY: number }[] = [
    { name: "hoardN", w: DOUBLES_HALF_WIDTH_METRES * 2 + 2, h: 1.1, pos: new Vector3(0, 0.55, z), rotY: Math.PI },
    { name: "hoardS", w: DOUBLES_HALF_WIDTH_METRES * 2 + 2, h: 1.1, pos: new Vector3(0, 0.55, -z), rotY: 0 },
    { name: "hoardE", w: FULL_LENGTH_METRES + 2, h: 1.1, pos: new Vector3(x, 0.55, 0), rotY: -Math.PI / 2 },
    { name: "hoardW", w: FULL_LENGTH_METRES + 2, h: 1.1, pos: new Vector3(-x, 0.55, 0), rotY: Math.PI / 2 },
  ];
  for (const b of boards) {
    const mesh = MeshBuilder.CreatePlane(b.name, { width: b.w, height: b.h }, scene);
    mesh.position = b.pos;
    mesh.rotation.y = b.rotY;
    const mat = new StandardMaterial(`${b.name}Mat`, scene);
    const tex = new DynamicTexture(`${b.name}Tex`, { width: 1024, height: 128 }, scene, false);
    paintHoarding(tex);
    mat.diffuseTexture = tex;
    mat.emissiveColor = new Color3(0.1, 0.12, 0.07);
    mat.specularColor = Color3.Black();
    mesh.material = mat;
    mesh.parent = parent;
    mesh.isPickable = false;
  }
}

function paintHoarding(texture: DynamicTexture): void {
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  const w = texture.getSize().width;
  const h = texture.getSize().height;
  ctx.fillStyle = "#1a3d1f";
  ctx.fillRect(0, 0, w, h);
  ctx.fillStyle = "#c4a35a";
  ctx.fillRect(0, 0, w, 6);
  ctx.fillRect(0, h - 6, w, 6);
  ctx.fillStyle = "#f5f2ea";
  ctx.font = "bold 48px Montserrat, Arial, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  for (let x = 140; x < w; x += 280) {
    ctx.fillText("TENNISLY", x, h / 2);
  }
  texture.update();
}

function paintCrowd(texture: DynamicTexture): void {
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  const w = texture.getSize().width;
  const h = texture.getSize().height;
  ctx.fillStyle = "#2a323a";
  ctx.fillRect(0, 0, w, h);
  for (let i = 0; i < 3500; i++) {
    const shade = 55 + Math.floor(Math.random() * 100);
    ctx.fillStyle = `rgb(${shade},${shade - 5},${shade + 8})`;
    ctx.fillRect(Math.random() * w, Math.random() * h, 2 + Math.random() * 2, 3 + Math.random() * 4);
  }
  texture.update();
}

function buildSkyDome(scene: Scene, parent: Mesh): void {
  const sky = MeshBuilder.CreateSphere(
    "skyDome",
    { diameter: 160, segments: 24, sideOrientation: Mesh.BACKSIDE },
    scene,
  );
  sky.position.y = 12;
  const skyMat = new StandardMaterial("skyMat", scene);
  skyMat.diffuseColor = new Color3(0.55, 0.7, 0.88);
  skyMat.emissiveColor = new Color3(0.42, 0.55, 0.72);
  skyMat.disableLighting = true;
  sky.material = skyMat;
  sky.parent = parent;
  sky.isPickable = false;
}
