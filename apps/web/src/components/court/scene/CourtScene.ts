import type { ReplayFrame, ShotSummary, Surface } from "@/types/replay";
import { interpolateAtTime } from "@/lib/replay-space";
import { usePlayback } from "@/stores/playback";
import { useReplaySession } from "@/stores/replaySession";
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
import { buildPostProcess, type CourtPostProcess } from "./buildPostProcess";
import { CameraDirector } from "./CameraDirector";
import {
  CAMERA_PRESETS,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "./cameraPresets";
import { hasStadiumModel, loadStadium } from "./loadStadium";
import { ReplayActors } from "./ReplayActors";
import type { PlayerGender } from "./loadPlayer";
import { PositioningHeatmaps } from "./PositioningHeatmaps";
import { ShotOverlays } from "./ShotOverlays";
import { buildSwingCues } from "./swingCues";

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
  readonly postProcess: CourtPostProcess;
  private disposed = false;
  private digiboards: DigiboardBuild | null = null;
  private actors: ReplayActors | null = null;
  private overlays: ShotOverlays | null = null;
  private heatmaps: PositioningHeatmaps | null = null;
  private frames: ReplayFrame[] = [];
  private boundShots: ShotSummary[] | null = null;
  private lastShotIndex = -1;
  private lastOverlayKey = "";

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
    this.postProcess = buildPostProcess(this.scene, this.camera);
    this.actors = new ReplayActors(this.scene, {
      homeGender: options.homeGender,
      awayGender: options.awayGender,
      shadows: this.lighting.shadows,
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
          this.enableStadiumShadowReceivers(stadium.root);
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
      this.bindReplayIfChanged();
      const dt = Math.min(this.engine.getDeltaTime() / 1000, 0.1);
      const session = useReplaySession.getState();
      const overlayKey =
        `${session.overlays.arcs}|${session.overlays.landings}|${session.overlays.serveBox}|` +
        `${session.overlays.heatmapHome}|${session.overlays.heatmapAway}`;
      if (overlayKey !== this.lastOverlayKey) {
        this.lastOverlayKey = overlayKey;
        this.overlays?.setVisibility(session.overlays);
        this.heatmaps?.setVisibility(session.overlays);
      }
      if (this.actors && this.frames.length > 0) {
        const playback = usePlayback.getState();
        const framePose = interpolateAtTime(this.frames, playback.timeSeconds);
        if (framePose) {
          this.actors.apply(framePose, dt, playback.playing ? playback.speed : 0);
          if (framePose.shotIndex !== this.lastShotIndex) {
            this.lastShotIndex = framePose.shotIndex;
            this.overlays?.setActiveShot(framePose.shotIndex);
          }
        }
      }
      this.scene.render();
    });

    if (process.env.NODE_ENV === "development") {
      (window as unknown as { __tennislyCourt?: CourtScene }).__tennislyCourt = this;
    }
  }

  private enableStadiumShadowReceivers(root: Mesh): void {
    for (const mesh of root.getChildMeshes(false)) {
      if (mesh.getTotalVertices() === 0) continue;
      const box = mesh.getBoundingInfo().boundingBox;
      if (box.maximumWorld.y < 1.5) {
        mesh.receiveShadows = true;
      }
    }
  }

  private bindReplayIfChanged(): void {
    const session = useReplaySession.getState();
    if (session.frames === this.frames && session.shots === this.boundShots) return;
    this.frames = session.frames;
    this.boundShots = session.shots;
    this.overlays?.dispose();
    this.heatmaps?.dispose();
    this.overlays = null;
    this.heatmaps = null;
    this.lastShotIndex = -1;
    this.lastOverlayKey = "";
    if (session.shots.length === 0 && session.frames.length === 0) {
      this.actors?.setSwingCues([]);
      return;
    }
    this.overlays = session.shots.length > 0 ? new ShotOverlays(this.scene, session.shots) : null;
    this.heatmaps =
      session.frames.length > 0 ? new PositioningHeatmaps(this.scene, session.frames) : null;
    this.heatmaps?.setVisibility(session.overlays);
    this.actors?.setSwingCues(buildSwingCues(session.frames, session.shots));
    const first = interpolateAtTime(this.frames, usePlayback.getState().timeSeconds);
    if (first) {
      this.actors?.apply(first);
      this.overlays?.setActiveShot(first.shotIndex);
      this.lastShotIndex = first.shotIndex;
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
    this.overlays?.dispose();
    this.overlays = null;
    this.heatmaps?.dispose();
    this.heatmaps = null;
    this.actors?.dispose();
    this.actors = null;
    this.digiboards?.dispose();
    this.digiboards = null;
    this.postProcess.dispose();
    this.engine.stopRenderLoop();
    this.scene.dispose();
    this.engine.dispose();
  }
}
