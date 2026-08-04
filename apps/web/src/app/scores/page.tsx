import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { ScoreboardSkeleton } from "@/components/scaffolds/ScoreboardSkeleton";
import { getScoreboardDay } from "@/services/scaffolds";

const subnav = [
  { id: "all", label: "All Scores", href: "/scores" },
  { id: "live", label: "Live", href: "/scores?status=live" },
  { id: "upcoming", label: "Upcoming", href: "/scores?status=upcoming" },
  { id: "final", label: "Results", href: "/scores?status=final" },
];

type PageProps = {
  searchParams: Promise<{ status?: string }>;
};

export default async function ScoresPage({ searchParams }: PageProps) {
  const { status } = await searchParams;
  const active =
    status === "live" || status === "upcoming" || status === "final" ? status : "all";
  const day = await getScoreboardDay(active === "all" ? undefined : active);

  return (
    <>
      <SectionSubnav items={subnav} activeId={active} />
      <ScoreboardSkeleton day={day} />
    </>
  );
}
