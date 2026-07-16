import type { Surface } from "@/types/replay";
import {
  ArcRotateCamera,
  Engine,
  type Mesh,
  Scene,
} from "@babylonjs/core";
import { buildCourt } from "./buildCourt";
import { buildDigiboards, type DigiboardBuild } from "./buildDigiboards";
import { buildEnvironment } from "./buildEnvironment";
import { buildLighting, type CourtLighting } from "./buildLighting";
import { CameraDirector } from "./CameraDirector";
import {
  CAMERA_PRESETS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "./cameraPresets";
import { hasStadiumModel, loadStadium } from "./loadStadium";

export type CourtSceneOptions = {
  canvas: HTMLCanvasElement;
  surface?: Surface;
  cameraPreset?: CameraPresetId;
  onReady?: () => void;
};

export class CourtScene {
  readonly engine: Engine;
  readonly scene: Scene;
  readonly camera: ArcRotateCamera;
  readonly lighting: CourtLighting;
  readonly courtRoot: Mesh;
  readonly cameraDirector: CameraDirector;
  private disposed = false;
  private digiboards: DigiboardBuild | null = null;

  constructor(options: CourtSceneOptions) {
    const surface = options.surface ?? "GRASS";
    const initialPreset = options.cameraPreset ?? DEFAULT_CAMERA_PRESET;
    this.engine = new Engine(options.canvas, true, {
      preserveDrawingBuffer: true,
      adaptToDeviceRatio: true,
      antialias: true,
    });
    this.scene = new Scene(this.engine);

    const pose = CAMERA_PRESETS[initialPreset];
    this.camera = new ArcRotateCamera(
      "broadcastCam",
      pose.alpha,
      pose.beta,
      pose.radius,
      pose.target.clone(),
      this.scene,
    );
    this.camera.lowerRadiusLimit = 12;
    this.camera.upperRadiusLimit = 55;
    this.camera.lowerBetaLimit = 0.25;
    this.camera.upperBetaLimit = Math.PI / 2 - 0.06;
    this.camera.wheelPrecision = 40;
    this.camera.panningSensibility = 0;
    this.camera.minZ = 0.1;
    this.camera.attachControl(options.canvas, true);
    this.cameraDirector = new CameraDirector(this.camera, this.scene);

    const court = buildCourt(this.scene, surface);
    this.courtRoot = court.root;
    this.lighting = buildLighting(this.scene, court.shadowCasters);

    if (hasStadiumModel(surface)) {
      this.courtRoot.position.y = 0.02;
      void loadStadium(this.scene, surface)
        .then((stadium) => {
          if (this.disposed || !stadium) return;
          if (stadium.alignedTo) {
            if (surface === "CLAY") {
              stadium.root
                .getChildMeshes(false)
                .find((m) => m.name === stadium.alignedTo)
                ?.setEnabled(false);
            } else {
              for (const name of ["apron", "courtInBounds"]) {
                this.courtRoot
                  .getChildMeshes(false)
                  .find((m) => m.name === name)
                  ?.setEnabled(false);
              }
            }
          }
          if (!this.disposed && surface === "GRASS") {
            this.digiboards = buildDigiboards(this.scene);
          }
          if (process.env.NODE_ENV === "development") {
            console.info(
              `[court] stadium aligned to "${stadium.alignedTo}": ` +
                `${stadium.courtLengthMetres.toFixed(2)}m x ${stadium.courtWidthMetres.toFixed(2)}m ` +
                `(scale ${stadium.scaleApplied.toFixed(3)})`,
            );
          }
          options.onReady?.();
        })
        .catch((err) => {
          if (process.env.NODE_ENV === "development") {
            console.warn("[court] stadium model failed to load, procedural fallback", err);
          }
          if (!this.disposed) {
            buildEnvironment(this.scene);
            if (surface === "GRASS") {
              this.digiboards = buildDigiboards(this.scene);
            }
          }
          options.onReady?.();
        });
    } else {
      buildEnvironment(this.scene);
      if (surface === "GRASS") {
        this.digiboards = buildDigiboards(this.scene);
      }
      options.onReady?.();
    }

    this.engine.runRenderLoop(() => {
      if (!this.disposed) this.scene.render();
    });

    if (process.env.NODE_ENV === "development") {
      (window as unknown as { __tennislyCourt?: CourtScene }).__tennislyCourt = this;
    }
  }

  setCameraPreset(preset: CameraPresetId, animate = true): void {
    this.cameraDirector.goTo(preset, animate ? 0.9 : 0);
  }

  resize(): void {
    this.engine.resize();
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.digiboards?.dispose();
    this.digiboards = null;
    this.engine.stopRenderLoop();
    this.scene.dispose();
    this.engine.dispose();
  }
}
