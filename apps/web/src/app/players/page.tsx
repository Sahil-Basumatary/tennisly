import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { PlayersSkeleton } from "@/components/scaffolds/PlayersSkeleton";
import { getPlayersBoard } from "@/services/scaffolds";

type PageProps = {
  searchParams: Promise<{ tour?: string; q?: string }>;
};

export default async function PlayersPage({ searchParams }: PageProps) {
  const { tour: tourParam, q } = await searchParams;
  const tour = tourParam === "wta" ? "wta" : "atp";
  const board = await getPlayersBoard(tour);
  const query = q?.trim().toLowerCase() ?? "";
  const rows = query
    ? board.rows.filter((row) => row.name.toLowerCase().includes(query))
    : board.rows;
  const subnav = [
    { id: "atp", label: "ATP Singles", href: "/players?tour=atp" },
    { id: "wta", label: "WTA Singles", href: "/players?tour=wta" },
  ];
  return (
    <>
      <SectionSubnav items={subnav} activeId={tour} />
      <PlayersSkeleton board={{ ...board, rows }} />
    </>
  );
}
