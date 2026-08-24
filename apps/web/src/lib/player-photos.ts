import { fetchWikiPlayerMedia, fetchWikiPlayerMediaMap } from "@/lib/wikipedia-upstream";
import type {
  MatchCentrePanel,
  PlayerProfile,
  PlayersBoard,
  ScoreboardDay,
  TournamentBoard,
} from "@/types/scaffolds";
import type { ScoreCard, ScoresFeed } from "@/types/scores";

export async function headshotByName(names: string[]): Promise<Map<string, string | null>> {
  const media = await fetchWikiPlayerMediaMap(names);
  const photos = new Map<string, string | null>();
  for (const name of names) {
    const key = name.trim();
    if (!key) continue;
    photos.set(key, media.get(key)?.imageSrc ?? null);
  }
  return photos;
}

function stampSide<T extends { name: string; photoUrl?: string | null }>(
  side: T,
  photos: Map<string, string | null>,
): T {
  return { ...side, photoUrl: photos.get(side.name) ?? null };
}

function stampCard(card: ScoreCard, photos: Map<string, string | null>): ScoreCard {
  return {
    ...card,
    home: stampSide(card.home, photos),
    away: stampSide(card.away, photos),
  };
}

export async function withScoreHeadshots(cards: ScoreCard[]): Promise<ScoreCard[]> {
  const photos = await headshotByName(cards.flatMap((card) => [card.home.name, card.away.name]));
  return cards.map((card) => stampCard(card, photos));
}

export async function withScoresFeedHeadshots(feed: ScoresFeed): Promise<ScoresFeed> {
  return { ...feed, items: await withScoreHeadshots(feed.items) };
}

export async function withScoreboardHeadshots(day: ScoreboardDay): Promise<ScoreboardDay> {
  const cards = day.groups.flatMap((group) => group.matches);
  const stamped = await withScoreHeadshots(cards);
  const byId = new Map(stamped.map((card) => [card.id, card]));
  return {
    ...day,
    groups: day.groups.map((group) => ({
      ...group,
      matches: group.matches.map((card) => byId.get(card.id) ?? card),
    })),
  };
}

export async function withPlayersBoardHeadshots(board: PlayersBoard): Promise<PlayersBoard> {
  const photos = await headshotByName(board.rows.map((row) => row.name));
  return {
    ...board,
    rows: board.rows.map((row) => ({ ...row, photoUrl: photos.get(row.name) ?? null })),
  };
}

export async function withTournamentHeadshots(board: TournamentBoard): Promise<TournamentBoard> {
  const photos = await headshotByName([
    ...board.standings.map((row) => row.player),
    ...board.fixtures.flatMap((row) => [row.home, row.away]),
  ]);
  return {
    ...board,
    standings: board.standings.map((row) => ({
      ...row,
      photoUrl: photos.get(row.player) ?? null,
    })),
    fixtures: board.fixtures.map((row) => ({
      ...row,
      homePhotoUrl: photos.get(row.home) ?? null,
      awayPhotoUrl: photos.get(row.away) ?? null,
    })),
  };
}

export async function withMatchCentreHeadshots(
  panel: MatchCentrePanel,
): Promise<MatchCentrePanel> {
  const photos = await headshotByName([panel.home.name, panel.away.name]);
  return {
    ...panel,
    home: { ...panel.home, photoUrl: photos.get(panel.home.name) ?? null },
    away: { ...panel.away, photoUrl: photos.get(panel.away.name) ?? null },
  };
}

export async function withPlayerProfileHeadshots(
  player: PlayerProfile,
): Promise<PlayerProfile> {
  const [portrait, matches] = await Promise.all([
    fetchWikiPlayerMedia(player.name),
    withScoreHeadshots(player.matches),
  ]);
  return { ...player, photoUrl: portrait.imageSrc, matches };
}
