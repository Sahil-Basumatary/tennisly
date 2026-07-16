import type { Surface } from "@/types/replay";
import { HALF_LENGTH_METRES } from "@/lib/court-geometry";
import {
  ArcRotateCamera,
  Engine,
  type Mesh,
  Scene,
  Vector3,
} from "@babylonjs/core";
import { buildCourt } from "./buildCourt";
import { buildEnvironment } from "./buildEnvironment";
import { buildLighting, type CourtLighting } from "./buildLighting";
import { hasStadiumModel, loadStadium } from "./loadStadium";

export type CourtSceneOptions = {
  canvas: HTMLCanvasElement;
  surface?: Surface;
  onReady?: () => void;
};

export class CourtScene {
  readonly engine: Engine;
  readonly scene: Scene;
  readonly camera: ArcRotateCamera;
  readonly lighting: CourtLighting;
  readonly courtRoot: Mesh;
  private disposed = false;

  constructor(options: CourtSceneOptions) {
    const surface = options.surface ?? "GRASS";
    this.engine = new Engine(options.canvas, true, {
      preserveDrawingBuffer: true,
      adaptToDeviceRatio: true,
      antialias: true,
    });
    this.scene = new Scene(this.engine);

    // TV broadcast frame: court fills most of the viewport, stands as a rim
    this.camera = new ArcRotateCamera(
      "broadcastCam",
      Math.PI / 2,
      1.1,
      HALF_LENGTH_METRES * 1.7,
      new Vector3(0, 0.2, 0),
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
              // Indoor bowl reused as clay venue — hide the model's blue court
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
          if (!this.disposed) buildEnvironment(this.scene);
          options.onReady?.();
        });
    } else {
      buildEnvironment(this.scene);
      options.onReady?.();
    }

    this.engine.runRenderLoop(() => {
      if (!this.disposed) this.scene.render();
    });

    if (process.env.NODE_ENV === "development") {
      (window as unknown as { __tennislyCourt?: CourtScene }).__tennislyCourt = this;
    }
  }

  resize(): void {
    this.engine.resize();
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.engine.stopRenderLoop();
    this.scene.dispose();
    this.engine.dispose();
  }
}
