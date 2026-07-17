import {
  Animation,
  type ArcRotateCamera,
  CircleEase,
  EasingFunction,
  type Scene,
  Vector3,
} from "@babylonjs/core";
import {
  CAMERA_PRESETS,
  type CameraPose,
  type CameraPresetId,
  DEFAULT_CAMERA_PRESET,
} from "./cameraPresets";

const FPS = 60;
const DEFAULT_DURATION_SEC = 0.9;

/**
 * Owns broadcast preset transitions on the arc-rotate camera while leaving
 * free orbit/zoom attached for manual control between presets.
 */
export class CameraDirector {
  private readonly camera: ArcRotateCamera;
  private readonly scene: Scene;
  private animating = false;

  constructor(camera: ArcRotateCamera, scene: Scene) {
    this.camera = camera;
    this.scene = scene;
    this.applyInstant(CAMERA_PRESETS[DEFAULT_CAMERA_PRESET]);
  }

  get isAnimating(): boolean {
    return this.animating;
  }

  goTo(preset: CameraPresetId, durationSec = DEFAULT_DURATION_SEC): void {
    const pose = CAMERA_PRESETS[preset];
    if (durationSec <= 0) {
      this.applyInstant(pose);
      return;
    }
    this.animateTo(pose, durationSec);
  }

  private applyInstant(pose: CameraPose): void {
    this.camera.alpha = pose.alpha;
    this.camera.beta = pose.beta;
    this.camera.radius = pose.radius;
    this.camera.setTarget(pose.target.clone());
  }

  private animateTo(pose: CameraPose, durationSec: number): void {
    const frames = Math.max(1, Math.round(durationSec * FPS));
    const ease = new CircleEase();
    ease.setEasingMode(EasingFunction.EASINGMODE_EASEINOUT);
    this.animating = true;

    const fromTarget = this.camera.getTarget().clone();
    const toTarget = pose.target.clone();
    const targetProxy = { x: fromTarget.x, y: fromTarget.y, z: fromTarget.z };

    const anims: Animation[] = [
      this.floatAnim("alpha", this.camera.alpha, pose.alpha, frames, ease),
      this.floatAnim("beta", this.camera.beta, pose.beta, frames, ease),
      this.floatAnim("radius", this.camera.radius, pose.radius, frames, ease),
      this.floatAnim("x", fromTarget.x, toTarget.x, frames, ease),
      this.floatAnim("y", fromTarget.y, toTarget.y, frames, ease),
      this.floatAnim("z", fromTarget.z, toTarget.z, frames, ease),
    ];

    this.scene.beginDirectAnimation(this.camera, anims.slice(0, 3), 0, frames, false);
    this.scene.beginDirectAnimation(targetProxy, anims.slice(3), 0, frames, false, 1, () => {
      this.animating = false;
      this.camera.setTarget(toTarget);
    });

    const observer = this.scene.onBeforeRenderObservable.add(() => {
      if (!this.animating) {
        this.scene.onBeforeRenderObservable.remove(observer);
        return;
      }
      this.camera.setTarget(new Vector3(targetProxy.x, targetProxy.y, targetProxy.z));
    });
  }

  private floatAnim(
    property: string,
    from: number,
    to: number,
    frames: number,
    ease: CircleEase,
  ): Animation {
    const anim = new Animation(
      `cam_${property}`,
      property,
      FPS,
      Animation.ANIMATIONTYPE_FLOAT,
      Animation.ANIMATIONLOOPMODE_CONSTANT,
    );
    anim.setKeys([
      { frame: 0, value: from },
      { frame: frames, value: to },
    ]);
    anim.setEasingFunction(ease);
    return anim;
  }
}
