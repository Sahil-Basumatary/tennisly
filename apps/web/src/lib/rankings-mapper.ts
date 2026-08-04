import type { UpstreamPlayer, UpstreamRanking } from "@/lib/tennis-data-upstream";
import type { PlayersBoard, StandingRow } from "@/types/scaffolds";

export function toPlayersBoard(
  rankings: UpstreamRanking[],
  players: UpstreamPlayer[],
  tour: "atp" | "wta",
): PlayersBoard {
  const nationalityById = new Map(
    players.map((player) => [player.id, player.nationality?.trim() || "—"] as const),
  );
  const rows = [...rankings]
    .sort((a, b) => a.rank - b.rank)
    .map((row) => ({
      id: row.playerId,
      rank: row.rank,
      name: row.playerName,
      country: nationalityById.get(row.playerId) ?? "—",
      points: row.points,
      href: `/players#${row.playerId}`,
    }));
  return {
    tour,
    updatedAt: new Date().toISOString(),
    rows,
  };
}

export function toStandingRows(rankings: UpstreamRanking[], limit = 8): StandingRow[] {
  return [...rankings]
    .sort((a, b) => a.rank - b.rank)
    .slice(0, limit)
    .map((row) => ({
      position: row.rank,
      player: row.playerName,
      played: 0,
      won: 0,
      lost: 0,
      points: row.points,
    }));
}
