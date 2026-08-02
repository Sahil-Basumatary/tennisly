import {
  Color3,
  Color4,
  CubeTexture,
  DirectionalLight,
  HDRCubeTexture,
  HemisphericLight,
  type Mesh,
  type Scene,
  ShadowGenerator,
  Vector3,
} from "@babylonjs/core";

export type CourtLighting = {
  keyLight: DirectionalLight;
  fillLight: HemisphericLight;
  shadows: ShadowGenerator;
  environment: CubeTexture | HDRCubeTexture;
};

const HDRI_URL = "/textures/court/stadium_01_2k.hdr";

export function buildLighting(scene: Scene, shadowCasters: Mesh[] = []): CourtLighting {
  scene.clearColor = new Color4(0.72, 0.8, 0.9, 1);

  const environment = new HDRCubeTexture(HDRI_URL, scene, 256, false, true, false, true);
  scene.environmentTexture = environment;
  // Sun-dominant budget: IBL above ~0.7 floods the shadow term until cast
  // shadows vanish, which is what made the court read flat and CG.
  scene.environmentIntensity = 0.55;

  const fillLight = new HemisphericLight("fill", new Vector3(0, 1, 0), scene);
  fillLight.intensity = 0.18;
  fillLight.groundColor = new Color3(0.25, 0.3, 0.22);
  fillLight.diffuse = new Color3(0.95, 0.96, 1);

  // Matches the sun position baked into the stadium HDRI so shadows agree with reflections
  const keyLight = new DirectionalLight("key", new Vector3(-0.4, -0.75, 0.5), scene);
  keyLight.position = new Vector3(22, 38, -26);
  keyLight.intensity = 3.4;
  keyLight.diffuse = new Color3(1, 0.96, 0.88);
  keyLight.shadowEnabled = true;
  // Fixed 32m frustum over the playing area: auto-extends refits every frame as
  // players move, which makes shadow edges swim. Fixed also buys ~64px/m.
  keyLight.autoUpdateExtends = false;
  keyLight.shadowMinZ = 10;
  keyLight.shadowMaxZ = 95;
  keyLight.orthoLeft = -16;
  keyLight.orthoRight = 16;
  keyLight.orthoTop = 16;
  keyLight.orthoBottom = -16;

  const shadows = new ShadowGenerator(2048, keyLight);
  shadows.usePercentageCloserFiltering = true;
  shadows.filteringQuality = ShadowGenerator.QUALITY_HIGH;
  shadows.setDarkness(0.25);
  shadows.bias = 0.0006;
  shadows.normalBias = 0.02;
  shadows.transparencyShadow = true;
  for (const mesh of shadowCasters) {
    shadows.addShadowCaster(mesh);
  }

  return { keyLight, fillLight, shadows, environment };
}
