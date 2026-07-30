import type { ShotType } from "@/types/replay";

const LABELS: Record<ShotType, string> = {
  FIRST_SERVE: "1st serve",
  SECOND_SERVE: "2nd serve",
  FOREHAND_GROUNDSTROKE: "Forehand",
  BACKHAND_GROUNDSTROKE: "Backhand",
  FOREHAND_VOLLEY: "FH volley",
  BACKHAND_VOLLEY: "BH volley",
  FOREHAND_SLICE: "FH slice",
  BACKHAND_SLICE: "BH slice",
  DROP_SHOT: "Drop shot",
  LOB: "Lob",
  OVERHEAD: "Overhead",
};

export function formatShotType(shotType: ShotType): string {
  return LABELS[shotType] ?? shotType.replaceAll("_", " ").toLowerCase();
}
