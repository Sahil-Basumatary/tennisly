import { getScoresFeed as getCatalogueScoresFeed } from "@/services/catalogue";
import type { ScoresFeed } from "@/types/scores";

/** Live match-service feed (ESPN-style strip + live centre). */
export async function getScoresFeed(uiStatus?: string): Promise<ScoresFeed> {
  return getCatalogueScoresFeed(uiStatus);
}
