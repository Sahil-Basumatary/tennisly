import { BALL_RADIUS_METRES } from "@/lib/court-geometry";
import {
  Color3,
  Mesh,
  MeshBuilder,
  type Scene,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";

const SEGMENTS = 26;
const HEAD_RADIUS = BALL_RADIUS_METRES * 0.85;
const MIN_SEGMENT_METRES = 0.06;

/**
 * Hawk-Eye style comet trail: a tube swept through the ball's recent path,
 * tapering to nothing at the tail. Rebuilt in place each frame via the tube
 * instance so no geometry is reallocated during playback.
 */
export class BallTrail {
  private readonly path: Vector3[];
  private tube: Mesh;
  private readonly material: StandardMaterial;
  private primed = false;

  constructor(scene: Scene, parent: Mesh) {
    this.path = Array.from({ length: SEGMENTS }, () => new Vector3(0, BALL_RADIUS_METRES, 0));
    this.material = new StandardMaterial("ballTrailMat", scene);
    this.material.diffuseColor = new Color3(1, 0.88, 0.25);
    this.material.emissiveColor = new Color3(0.95, 0.72, 0.12);
    this.material.specularColor = Color3.Black();
    this.material.disableLighting = true;
    this.material.alpha = 0.62;
    this.material.backFaceCulling = false;

    this.tube = MeshBuilder.CreateTube(
      "ballTrail",
      { path: this.path, radiusFunction: taper, tessellation: 8, updatable: true, cap: Mesh.NO_CAP },
      scene,
    );
    this.tube.material = this.material;
    this.tube.parent = parent;
    this.tube.isPickable = false;
    this.tube.setEnabled(false);
  }

  /** Pushes the ball's current position onto the head of the trail. */
  update(position: Vector3): void {
    if (!this.primed) {
      for (const point of this.path) {
        point.copyFrom(position);
      }
      this.primed = true;
      this.tube.setEnabled(true);
    } else if (Vector3.DistanceSquared(this.path[SEGMENTS - 1], position) < MIN_SEGMENT_METRES ** 2) {
      // Ball barely moved (paused or between-point idle) — keep the tail static.
      this.path[SEGMENTS - 1].copyFrom(position);
    } else {
      for (let i = 0; i < SEGMENTS - 1; i++) {
        this.path[i].copyFrom(this.path[i + 1]);
      }
      this.path[SEGMENTS - 1].copyFrom(position);
    }
    this.tube = MeshBuilder.CreateTube(
      "ballTrail",
      { path: this.path, radiusFunction: taper, instance: this.tube },
      undefined,
    );
  }

  reset(): void {
    this.primed = false;
    this.tube.setEnabled(false);
  }

  dispose(): void {
    this.tube.dispose();
    this.material.dispose();
  }
}

function taper(index: number): number {
  const t = index / (SEGMENTS - 1);
  return HEAD_RADIUS * t * t;
}
