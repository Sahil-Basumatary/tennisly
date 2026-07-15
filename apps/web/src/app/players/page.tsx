import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { PlayersSkeleton } from "@/components/scaffolds/PlayersSkeleton";
import { getPlayersBoard } from "@/services/scaffolds";

const subnav = [
  { id: "rankings", label: "Rankings", href: "/players" },
  { id: "search", label: "Search", href: "/players?view=search" },
];

export default async function PlayersPage() {
  const board = await getPlayersBoard();

  return (
    <>
      <SectionSubnav items={subnav} activeId="rankings" />
      <PlayersSkeleton board={board} />
    </>
  );
}
