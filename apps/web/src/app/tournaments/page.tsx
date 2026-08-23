import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { TournamentSkeleton } from "@/components/scaffolds/TournamentSkeleton";
import {
  parseTournamentQuery,
  tournamentActiveId,
} from "@/lib/tournament-filter";
import { getTournamentBoard } from "@/services/scaffolds";

const subnav = [
  { id: "overview", label: "Overview", href: "/tournaments" },
  { id: "atp", label: "ATP", href: "/tournaments?tour=atp" },
  { id: "wta", label: "WTA", href: "/tournaments?tour=wta" },
  { id: "slams", label: "Grand Slams", href: "/tournaments?level=grand_slam" },
  { id: "davis", label: "Davis Cup", href: "/tournaments?name=davis" },
  { id: "bjk", label: "BJK Cup", href: "/tournaments?name=bjk" },
];

type PageProps = {
  searchParams: Promise<{ tour?: string; level?: string; name?: string }>;
};

export default async function TournamentsPage({ searchParams }: PageProps) {
  const params = await searchParams;
  const query = parseTournamentQuery(params);
  const board = await getTournamentBoard(query);
  return (
    <>
      <SectionSubnav items={subnav} activeId={tournamentActiveId(query)} />
      <TournamentSkeleton board={board} />
    </>
  );
}
