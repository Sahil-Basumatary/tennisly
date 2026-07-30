import { landingIsIn, serviceBoxCentre } from "@/lib/court-bounds";
import {
  SERVICE_LINE_FROM_NET_METRES,
  SINGLES_HALF_WIDTH_METRES,
} from "@/lib/court-geometry";
import { shotArcPoint, toBabylon } from "@/lib/replay-space";
import type { ShotSummary } from "@/types/replay";
import {
  Color3,
  Mesh,
  MeshBuilder,
  type Scene,
  StandardMaterial,
  Vector3,
} from "@babylonjs/core";

const ARC_SAMPLES = 28;
const ARC_RADIUS = 0.045;
const LANDING_RADIUS = 0.28;

type ShotVisual = {
  arc: Mesh;
  landing: Mesh;
  serveBox: Mesh | null;
};

export type ShotOverlayVisibility = {
  arcs: boolean;
  landings: boolean;
  serveBox: boolean;
};

/**
 * Broadcast-style shot overlays: path tubes, landing discs, serve-box highlight.
 */
export class ShotOverlays {
  readonly root: Mesh;
  private readonly visuals: ShotVisual[] = [];
  private activeIndex = 0;
  private visibility: ShotOverlayVisibility = {
    arcs: true,
    landings: true,
    serveBox: true,
  };

  constructor(scene: Scene, shots: ShotSummary[]) {
    this.root = new Mesh("shotOverlays", scene);
    for (let i = 0; i < shots.length; i++) {
      this.visuals.push(buildShotVisual(scene, this.root, shots[i], i));
    }
    this.setActiveShot(0);
    this.applyVisibility();
  }

  setActiveShot(index: number): void {
    this.activeIndex = index;
    for (let i = 0; i < this.visuals.length; i++) {
      const v = this.visuals[i];
      const active = i === index;
      const arcMat = v.arc.material as StandardMaterial;
      arcMat.alpha = active ? 0.92 : 0.28;
      arcMat.emissiveColor = active
        ? new Color3(1, 0.92, 0.25)
        : new Color3(0.55, 0.55, 0.2);
      const landMat = v.landing.material as StandardMaterial;
      landMat.alpha = active ? 0.85 : 0.35;
      if (v.serveBox) {
        v.serveBox.setEnabled(active && this.visibility.serveBox);
        const boxMat = v.serveBox.material as StandardMaterial;
        boxMat.alpha = active ? 0.28 : 0.1;
      }
    }
  }

  setVisibility(flags: ShotOverlayVisibility): void {
    this.visibility = flags;
    this.applyVisibility();
    this.setActiveShot(this.activeIndex);
  }

  private applyVisibility(): void {
    for (const v of this.visuals) {
      v.arc.setEnabled(this.visibility.arcs);
      v.landing.setEnabled(this.visibility.landings);
      if (v.serveBox) {
        v.serveBox.setEnabled(this.visibility.serveBox);
      }
    }
  }

  dispose(): void {
    this.root.dispose();
  }
}

function buildShotVisual(
  scene: Scene,
  parent: Mesh,
  shot: ShotSummary,
  index: number,
): ShotVisual {
  const path: Vector3[] = [];
  for (let s = 0; s <= ARC_SAMPLES; s++) {
    const t = s / ARC_SAMPLES;
    const p = toBabylon(shotArcPoint(shot.contact, shot.landing, shot.apexHeightMetres, t));
    path.push(new Vector3(p.x, p.y, p.z));
  }

  const arc = MeshBuilder.CreateTube(
    `shotArc_${index}`,
    { path, radius: ARC_RADIUS, tessellation: 8, updatable: false },
    scene,
  );
  const arcMat = new StandardMaterial(`shotArcMat_${index}`, scene);
  arcMat.diffuseColor = new Color3(0.95, 0.85, 0.15);
  arcMat.emissiveColor = new Color3(0.7, 0.55, 0.1);
  arcMat.specularColor = new Color3(0.1, 0.1, 0.05);
  arcMat.alpha = 0.28;
  arcMat.backFaceCulling = false;
  arc.material = arcMat;
  arc.parent = parent;
  arc.isPickable = false;

  const land = toBabylon(shot.landing);
  const inBounds = landingIsIn(shot.shotType, shot.landing, shot.hitter);
  const landing = MeshBuilder.CreateDisc(
    `shotLanding_${index}`,
    { radius: LANDING_RADIUS, tessellation: 28 },
    scene,
  );
  landing.position.set(land.x, 0.03, land.z);
  landing.rotation.x = Math.PI / 2;
  const landMat = new StandardMaterial(`shotLandingMat_${index}`, scene);
  landMat.diffuseColor = inBounds
    ? new Color3(0.15, 0.85, 0.35)
    : new Color3(0.9, 0.15, 0.12);
  landMat.emissiveColor = inBounds
    ? new Color3(0.05, 0.35, 0.12)
    : new Color3(0.4, 0.05, 0.04);
  landMat.alpha = 0.35;
  landMat.disableLighting = false;
  landing.material = landMat;
  landing.parent = parent;
  landing.isPickable = false;

  let serveBox: Mesh | null = null;
  if (shot.shotType.includes("SERVE")) {
    const centre = serviceBoxCentre(shot.landing, shot.hitter);
    if (centre) {
      const c = toBabylon(centre);
      serveBox = MeshBuilder.CreateGround(
        `serveBox_${index}`,
        {
          width: SINGLES_HALF_WIDTH_METRES,
          height: SERVICE_LINE_FROM_NET_METRES,
        },
        scene,
      );
      serveBox.position.set(c.x, 0.025, c.z);
      const boxMat = new StandardMaterial(`serveBoxMat_${index}`, scene);
      boxMat.diffuseColor = new Color3(0.2, 0.55, 1);
      boxMat.emissiveColor = new Color3(0.08, 0.22, 0.45);
      boxMat.alpha = 0.28;
      boxMat.backFaceCulling = false;
      serveBox.material = boxMat;
      serveBox.parent = parent;
      serveBox.isPickable = false;
      serveBox.setEnabled(false);
    }
  }

  return { arc, landing, serveBox };
}
