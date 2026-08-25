import { playerCountry, publicPlayerName } from "@/lib/player-directory";
import type { UpstreamRanking } from "@/lib/tennis-data-upstream";
import type { PlayersBoard, StandingRow } from "@/types/scaffolds";

export function toPlayersBoard(
  rankings: UpstreamRanking[],
  tour: "atp" | "wta",
): PlayersBoard {
  const rows = [...rankings]
    .sort((a, b) => a.rank - b.rank)
    .map((row) => ({
      id: row.playerId,
      rank: row.rank,
      name: publicPlayerName(row.playerName),
      country: playerCountry(row.nationality),
      points: row.points,
      href: `/players/${row.playerId}`,
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
      player: publicPlayerName(row.playerName),
      points: row.points,
    }));
}
