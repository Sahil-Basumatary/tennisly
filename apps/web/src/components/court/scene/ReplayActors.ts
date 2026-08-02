import { BALL_RADIUS_METRES } from "@/lib/court-geometry";
import { toBabylon, type InterpolatedPose } from "@/lib/replay-space";
import {
  Color3,
  DynamicTexture,
  Mesh,
  MeshBuilder,
  PBRMaterial,
  type Scene,
  type ShadowGenerator,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";
import { BallTrail } from "./BallTrail";
import { loadPlayer, type LoadedPlayer, type PlayerGender } from "./loadPlayer";
import {
  PlayerAnimator,
  SWING_FOLLOW_SECONDS,
  SWING_LEAD_SECONDS,
  type SwingPhase,
} from "./PlayerAnimator";
import type { SwingCue } from "./swingCues";

/** Athletes pivot fast but not instantly; unclamped yaw snapped 180° in a frame. */
const TURN_RATE_RAD_PER_SEC = 9;
/** Smoothing constant for the ground-speed estimate feeding the jog blend. */
const SPEED_SMOOTHING_TAU = 0.12;
/** Above a flat-out sprint the delta came from a seek, not from running. */
const MAX_TRACKED_SPEED_MPS = 12;
/** Clock drift beyond this is a scrub rather than normal frame-to-frame advance. */
const SEEK_TOLERANCE_SECONDS = 0.05;

export type ReplayActorsOptions = {
  homeGender?: PlayerGender;
  awayGender?: PlayerGender;
  shadows?: ShadowGenerator;
};

export class ReplayActors {
  readonly root: Mesh;
  private readonly ball: Mesh;
  private readonly ballShadow: Mesh;
  private readonly trail: BallTrail;
  private home: LoadedPlayer | null = null;
  private away: LoadedPlayer | null = null;
  private homeAnimator: PlayerAnimator | null = null;
  private awayAnimator: PlayerAnimator | null = null;
  private prevHome = new Vector3(0, 0, 0);
  private prevAway = new Vector3(0, 0, 0);
  private homeSpeed = 0;
  private awaySpeed = 0;
  private prevTime = 0;
  private cues: SwingCue[] = [];
  private ready = false;
  private readonly homeGender: PlayerGender;
  private readonly awayGender: PlayerGender;
  private readonly shadows: ShadowGenerator | null;

  constructor(scene: Scene, options: ReplayActorsOptions = {}) {
    this.homeGender = options.homeGender ?? "male";
    this.awayGender = options.awayGender ?? "male";
    this.shadows = options.shadows ?? null;
    this.root = new Mesh("replayActors", scene);

    // Felt ball reads as a matte dielectric under ACES; emissive fakes the
    // stadium-light rim that broadcast cameras always pick up on a live ball.
    const ballMat = new PBRMaterial("ballMat", scene);
    ballMat.albedoColor = new Color3(0.85, 0.88, 0.16);
    ballMat.emissiveColor = new Color3(0.16, 0.17, 0.02);
    ballMat.metallic = 0;
    ballMat.roughness = 0.85;
    this.ball = MeshBuilder.CreateSphere(
      "ball",
      { diameter: BALL_RADIUS_METRES * 2, segments: 20 },
      scene,
    );
    this.ball.material = ballMat;
    this.ball.parent = this.root;
    this.ball.isPickable = false;

    const shadowMat = new StandardMaterial("ballShadowMat", scene);
    shadowMat.diffuseTexture = createSoftShadowTexture(scene);
    shadowMat.opacityTexture = shadowMat.diffuseTexture;
    shadowMat.disableLighting = true;
    shadowMat.emissiveColor = Color3.Black();
    shadowMat.specularColor = Color3.Black();
    this.ballShadow = MeshBuilder.CreatePlane(
      "ballShadow",
      { size: BALL_RADIUS_METRES * 9 },
      scene,
    );
    this.ballShadow.rotation.x = Math.PI / 2;
    this.ballShadow.material = shadowMat;
    this.ballShadow.parent = this.root;
    this.ballShadow.isPickable = false;

    this.trail = new BallTrail(scene, this.root);

    void this.loadAthletes(scene);
  }

  private async loadAthletes(scene: Scene): Promise<void> {
    try {
      this.home = await loadPlayer(scene, this.homeGender, "homePlayer", "home");
      this.away = await loadPlayer(scene, this.awayGender, "awayPlayer", "away");
      this.home.root.parent = this.root;
      this.away.root.parent = this.root;
      for (const player of [this.home, this.away]) {
        for (const mesh of player.root.getChildMeshes(false)) {
          this.shadows?.addShadowCaster(mesh);
          mesh.receiveShadows = true;
        }
      }
      this.homeAnimator = new PlayerAnimator(this.home.clips);
      this.awayAnimator = new PlayerAnimator(this.away.clips);
      this.ready = true;
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.warn("[court] player models failed to load", err);
      }
    }
  }

  setSwingCues(cues: SwingCue[]): void {
    this.cues = cues;
  }

  apply(pose: InterpolatedPose, deltaSeconds = 0, timeScale = 0): void {
    const ball = toBabylon(pose.ball);
    this.ball.position.set(ball.x, ball.y, ball.z);
    this.ballShadow.position.set(ball.x, 0.012, ball.z);
    // Contact shadows tighten and darken as the ball nears the surface.
    const height = Math.max(0, ball.y);
    this.ballShadow.scaling.setAll(0.55 + Math.min(height, 6) * 0.14);
    this.ballShadow.visibility = Math.max(0.12, 0.75 - height * 0.09);
    this.trail.update(this.ball.position);

    if (!this.ready || !this.home || !this.away || !this.homeAnimator || !this.awayAnimator) {
      return;
    }

    const homePos = toBabylon(pose.home);
    const awayPos = toBabylon(pose.away);
    this.home.root.position.set(homePos.x, this.home.footOffsetY, homePos.z);
    this.away.root.position.set(awayPos.x, this.away.footOffsetY, awayPos.z);

    turnToward(this.home.root, ball.x, ball.z, deltaSeconds);
    turnToward(this.away.root, ball.x, ball.z, deltaSeconds);

    const home = this.home.root.position;
    const away = this.away.root.position;
    this.homeSpeed = this.trackSpeed(this.homeSpeed, this.prevHome, home, deltaSeconds);
    this.awaySpeed = this.trackSpeed(this.awaySpeed, this.prevAway, away, deltaSeconds);
    const expected = deltaSeconds * timeScale;
    const seeked = Math.abs(pose.timeSeconds - this.prevTime - expected) > SEEK_TOLERANCE_SECONDS;
    this.prevTime = pose.timeSeconds;

    this.homeAnimator.update(deltaSeconds, {
      swing: this.swingAt(pose.timeSeconds, "home"),
      groundSpeedMps: this.homeSpeed,
      timeScale,
      seeked,
    });
    this.awayAnimator.update(deltaSeconds, {
      swing: this.swingAt(pose.timeSeconds, "away"),
      groundSpeedMps: this.awaySpeed,
      timeScale,
      seeked,
    });
  }

  /** Nearest cue for this side whose stroke window covers the current time. */
  private swingAt(timeSeconds: number, side: "home" | "away"): SwingPhase | null {
    let best: SwingPhase | null = null;
    let bestDistance = Infinity;
    for (const cue of this.cues) {
      if (cue.side !== side) continue;
      const offset = timeSeconds - cue.timeSeconds;
      if (offset < -SWING_LEAD_SECONDS || offset > SWING_FOLLOW_SECONDS) continue;
      const distance = Math.abs(offset);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = { clip: cue.clip, secondsFromContact: offset };
      }
    }
    return best;
  }

  private trackSpeed(
    smoothed: number,
    previous: Vector3,
    current: Vector3,
    deltaSeconds: number,
  ): number {
    const travelled = Math.hypot(current.x - previous.x, current.z - previous.z);
    previous.copyFrom(current);
    if (deltaSeconds <= 0) return smoothed;
    const instant = travelled / deltaSeconds;
    // Scrubbing teleports the athlete, which would otherwise read as a sprint
    // and kick the jog blend in on a stationary player.
    if (instant > MAX_TRACKED_SPEED_MPS) return 0;
    const alpha = 1 - Math.exp(-deltaSeconds / SPEED_SMOOTHING_TAU);
    return smoothed + (instant - smoothed) * alpha;
  }

  dispose(): void {
    this.homeAnimator?.dispose();
    this.awayAnimator?.dispose();
    this.trail.dispose();
    this.root.dispose();
  }
}

/**
 * Radial falloff disc — a hard-edged disc reads as a sticker, while broadcast
 * ball shadows are soft penumbras from stadium floodlight arrays.
 */
function createSoftShadowTexture(scene: Scene): DynamicTexture {
  const size = 128;
  const texture = new DynamicTexture("ballShadowTex", { width: size, height: size }, scene, true);
  const ctx = texture.getContext() as unknown as CanvasRenderingContext2D;
  const half = size / 2;
  const gradient = ctx.createRadialGradient(half, half, 0, half, half, half);
  gradient.addColorStop(0, "rgba(0,0,0,0.9)");
  gradient.addColorStop(0.45, "rgba(0,0,0,0.45)");
  gradient.addColorStop(1, "rgba(0,0,0,0)");
  ctx.clearRect(0, 0, size, size);
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, size, size);
  texture.hasAlpha = true;
  texture.update();
  return texture;
}

function turnToward(
  root: { rotation: Vector3; position: Vector3 },
  x: number,
  z: number,
  deltaSeconds: number,
): void {
  const dx = x - root.position.x;
  const dz = z - root.position.z;
  if (dx * dx + dz * dz < 1e-6) return;
  const target = Math.atan2(dx, dz);
  if (deltaSeconds <= 0) {
    root.rotation.y = target;
    return;
  }
  const maxStep = TURN_RATE_RAD_PER_SEC * deltaSeconds;
  root.rotation.y += clampAngle(target - root.rotation.y, maxStep);
}

/** Wraps to the shortest arc so crossing ±π turns the near way, not a full spin. */
function clampAngle(delta: number, maxStep: number): number {
  const wrapped = Math.atan2(Math.sin(delta), Math.cos(delta));
  return Math.max(-maxStep, Math.min(maxStep, wrapped));
}
