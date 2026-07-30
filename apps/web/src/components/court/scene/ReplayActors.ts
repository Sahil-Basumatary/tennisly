import { BALL_RADIUS_METRES } from "@/lib/court-geometry";
import { toBabylon, type InterpolatedPose } from "@/lib/replay-space";
import type { ShotType } from "@/types/replay";
import {
  Color3,
  Mesh,
  MeshBuilder,
  type Scene,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";
import {
  loadPlayer,
  type LoadedPlayer,
  type PlayerClip,
  type PlayerGender,
} from "./loadPlayer";

const TRAIL_LENGTH = 18;
const MOVE_SPEED_JOG = 1.2;

export type ReplayActorsOptions = {
  homeGender?: PlayerGender;
  awayGender?: PlayerGender;
};

export class ReplayActors {
  readonly root: Mesh;
  private readonly ball: Mesh;
  private readonly ballShadow: Mesh;
  private readonly trail: Mesh[];
  private trailIndex = 0;
  private home: LoadedPlayer | null = null;
  private away: LoadedPlayer | null = null;
  private homeClip: PlayerClip | null = null;
  private awayClip: PlayerClip | null = null;
  private prevHome = new Vector3(0, 0, 0);
  private prevAway = new Vector3(0, 0, 0);
  private prevShot: ShotType | null = null;
  private ready = false;
  private readonly homeGender: PlayerGender;
  private readonly awayGender: PlayerGender;

  constructor(scene: Scene, options: ReplayActorsOptions = {}) {
    this.homeGender = options.homeGender ?? "male";
    this.awayGender = options.awayGender ?? "male";
    this.root = new Mesh("replayActors", scene);

    const ballMat = new StandardMaterial("ballMat", scene);
    ballMat.diffuseColor = new Color3(0.95, 0.92, 0.2);
    ballMat.emissiveColor = new Color3(0.35, 0.32, 0.05);
    ballMat.specularColor = new Color3(0.4, 0.4, 0.3);
    this.ball = MeshBuilder.CreateSphere(
      "ball",
      { diameter: BALL_RADIUS_METRES * 2, segments: 16 },
      scene,
    );
    this.ball.material = ballMat;
    this.ball.parent = this.root;
    this.ball.isPickable = false;

    const shadowMat = new StandardMaterial("ballShadowMat", scene);
    shadowMat.diffuseColor = new Color3(0, 0, 0);
    shadowMat.alpha = 0.35;
    shadowMat.disableLighting = true;
    this.ballShadow = MeshBuilder.CreateDisc(
      "ballShadow",
      { radius: BALL_RADIUS_METRES * 2.2, tessellation: 24 },
      scene,
    );
    this.ballShadow.rotation.x = Math.PI / 2;
    this.ballShadow.material = shadowMat;
    this.ballShadow.parent = this.root;
    this.ballShadow.isPickable = false;

    this.trail = [];
    const trailMat = new StandardMaterial("ballTrailMat", scene);
    trailMat.diffuseColor = new Color3(1, 0.85, 0.2);
    trailMat.emissiveColor = new Color3(0.8, 0.55, 0.05);
    trailMat.alpha = 0.55;
    trailMat.disableLighting = true;
    for (let i = 0; i < TRAIL_LENGTH; i++) {
      const bead = MeshBuilder.CreateSphere(
        `trail_${i}`,
        { diameter: BALL_RADIUS_METRES * 1.4, segments: 8 },
        scene,
      );
      bead.material = trailMat;
      bead.parent = this.root;
      bead.isPickable = false;
      bead.setEnabled(false);
      this.trail.push(bead);
    }

    void this.loadAthletes(scene);
  }

  private async loadAthletes(scene: Scene): Promise<void> {
    try {
      this.home = await loadPlayer(scene, this.homeGender, "homePlayer");
      this.away = await loadPlayer(scene, this.awayGender, "awayPlayer");
      this.home.root.parent = this.root;
      this.away.root.parent = this.root;
      this.playClip(this.home, "idle", true);
      this.playClip(this.away, "idle", true);
      this.homeClip = "idle";
      this.awayClip = "idle";
      this.ready = true;
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.warn("[court] player models failed to load", err);
      }
    }
  }

  apply(pose: InterpolatedPose): void {
    const ball = toBabylon(pose.ball);
    this.ball.position.set(ball.x, ball.y, ball.z);
    this.ballShadow.position.set(ball.x, 0.012, ball.z);
    const shadowScale = Math.max(0.35, 1 - ball.y * 0.18);
    this.ballShadow.scaling.setAll(shadowScale);

    const bead = this.trail[this.trailIndex % TRAIL_LENGTH];
    bead.position.set(ball.x, ball.y, ball.z);
    bead.setEnabled(true);
    this.trailIndex += 1;

    if (!this.ready || !this.home || !this.away) return;

    const homePos = toBabylon(pose.home);
    const awayPos = toBabylon(pose.away);
    this.home.root.position.set(homePos.x, this.home.footOffsetY, homePos.z);
    this.away.root.position.set(awayPos.x, this.away.footOffsetY, awayPos.z);

    faceToward(this.home.root, ball.x, ball.z);
    faceToward(this.away.root, ball.x, ball.z);

    const homeSpeed = horizontalSpeed(this.prevHome, this.home.root.position);
    const awaySpeed = horizontalSpeed(this.prevAway, this.away.root.position);
    this.prevHome.copyFrom(this.home.root.position);
    this.prevAway.copyFrom(this.away.root.position);

    const shotChanged = this.prevShot !== null && this.prevShot !== pose.shotType;
    this.prevShot = pose.shotType;
    const swing = shotTypeToClip(pose.shotType);

    if (shotChanged && swing) {
      if (pose.shotType.includes("SERVE") || hitterIsHome(pose)) {
        this.playClip(this.home, swing, false);
        this.homeClip = swing;
      } else {
        this.playClip(this.away, swing, false);
        this.awayClip = swing;
      }
    } else {
      this.ensureLoco(this.home, homeSpeed, "home");
      this.ensureLoco(this.away, awaySpeed, "away");
    }
  }

  private ensureLoco(player: LoadedPlayer, speed: number, side: "home" | "away"): void {
    const current = side === "home" ? this.homeClip : this.awayClip;
    if (current && current !== "idle" && current !== "jog") {
      const group = player.clips[current];
      if (group && group.isPlaying) return;
    }
    const next: PlayerClip = speed >= MOVE_SPEED_JOG ? "jog" : "idle";
    if (current === next) return;
    this.playClip(player, next, true);
    if (side === "home") this.homeClip = next;
    else this.awayClip = next;
  }

  private playClip(player: LoadedPlayer, clip: PlayerClip, loop: boolean): void {
    for (const group of Object.values(player.clips)) {
      group?.stop();
    }
    const group = player.clips[clip] ?? player.clips.idle;
    if (!group) return;
    group.loopAnimation = loop;
    group.start(loop, 1.0, group.from, group.to, false);
  }

  dispose(): void {
    this.root.dispose();
  }
}

function faceToward(root: { rotation: Vector3; position: Vector3 }, x: number, z: number): void {
  const dx = x - root.position.x;
  const dz = z - root.position.z;
  if (dx * dx + dz * dz < 1e-6) return;
  root.rotation.y = Math.atan2(dx, dz);
}

function horizontalSpeed(prev: Vector3, next: Vector3): number {
  const dx = next.x - prev.x;
  const dz = next.z - prev.z;
  return Math.hypot(dx, dz) * 60;
}

function shotTypeToClip(shot: ShotType): PlayerClip | null {
  if (shot.includes("SERVE")) return "serve";
  if (shot.includes("SMASH") || shot.includes("OVERHEAD")) return "smash";
  if (shot.includes("BACKHAND")) return "backhand";
  if (shot.includes("FOREHAND") || shot.includes("GROUND") || shot.includes("VOLLEY")) {
    return "forehand";
  }
  return "forehand";
}

function hitterIsHome(pose: InterpolatedPose): boolean {
  // Rough: home stands negative depth in our mock; refine when shot.hitter is on the frame
  return pose.home.y <= 0;
}
