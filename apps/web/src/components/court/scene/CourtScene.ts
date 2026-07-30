import type { ReplayFrame, Surface } from "@/types/replay";
import { interpolateAtTime } from "@/lib/replay-space";
import { getMatchReplay } from "@/services/replay";
import { usePlayback } from "@/stores/playback";
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
import { ReplayActors } from "./ReplayActors";
import type { PlayerGender } from "./loadPlayer";

export type CourtSceneOptions = {
  canvas: HTMLCanvasElement;
  surface?: Surface;
  cameraPreset?: CameraPresetId;
  homeGender?: PlayerGender;
  awayGender?: PlayerGender;
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
  private actors: ReplayActors | null = null;
  private frames: ReplayFrame[] = [];

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
    this.actors = new ReplayActors(this.scene, {
      homeGender: options.homeGender,
      awayGender: options.awayGender,
    });

    void getMatchReplay().then((replay) => {
      if (this.disposed) return;
      this.frames = replay.frames;
      usePlayback.getState().setDuration(replay.durationSeconds);
      const first = interpolateAtTime(this.frames, 0);
      if (first) this.actors?.apply(first);
    });

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
      if (this.disposed) return;
      const dt = this.engine.getDeltaTime() / 1000;
      usePlayback.getState().tick(dt);
      if (this.actors && this.frames.length > 0) {
        const pose = interpolateAtTime(this.frames, usePlayback.getState().timeSeconds);
        if (pose) this.actors.apply(pose);
      }
      this.scene.render();
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
    this.actors?.dispose();
    this.actors = null;
    this.digiboards?.dispose();
    this.digiboards = null;
    usePlayback.getState().pause();
    this.engine.stopRenderLoop();
    this.scene.dispose();
    this.engine.dispose();
  }
}
