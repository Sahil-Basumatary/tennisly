import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { PlayersSkeleton } from "@/components/scaffolds/PlayersSkeleton";
import { getPlayersBoard } from "@/services/scaffolds";

type PageProps = {
  searchParams: Promise<{ tour?: string }>;
};

export default async function PlayersPage({ searchParams }: PageProps) {
  const { tour: tourParam } = await searchParams;
  const tour = tourParam === "wta" ? "wta" : "atp";
  const board = await getPlayersBoard(tour);
  const subnav = [
    { id: "atp", label: "ATP Singles", href: "/players?tour=atp" },
    { id: "wta", label: "WTA Singles", href: "/players?tour=wta" },
  ];

  return (
    <>
      <SectionSubnav items={subnav} activeId={tour} />
      <PlayersSkeleton board={board} />
    </>
  );
}
