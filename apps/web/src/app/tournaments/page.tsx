import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { TournamentSkeleton } from "@/components/scaffolds/TournamentSkeleton";
import { getTournamentBoard } from "@/services/scaffolds";

const subnav = [
  { id: "overview", label: "Overview", href: "/tournaments" },
  { id: "atp", label: "ATP", href: "/tournaments?tour=atp" },
  { id: "wta", label: "WTA", href: "/tournaments?tour=wta" },
  { id: "slams", label: "Grand Slams", href: "/tournaments?level=grand_slam" },
];

export default async function TournamentsPage() {
  const board = await getTournamentBoard();

  return (
    <>
      <SectionSubnav items={subnav} activeId="overview" />
      <TournamentSkeleton board={board} />
    </>
  );
}
