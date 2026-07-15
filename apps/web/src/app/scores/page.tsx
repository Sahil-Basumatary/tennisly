import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { ScoreboardSkeleton } from "@/components/scaffolds/ScoreboardSkeleton";
import { getScoreboardDay } from "@/services/scaffolds";

const subnav = [
  { id: "all", label: "All Scores", href: "/scores" },
  { id: "live", label: "Live", href: "/scores?status=live" },
  { id: "upcoming", label: "Upcoming", href: "/scores?status=upcoming" },
  { id: "final", label: "Results", href: "/scores?status=final" },
];

export default async function ScoresPage() {
  const day = await getScoreboardDay();

  return (
    <>
      <SectionSubnav items={subnav} activeId="all" />
      <ScoreboardSkeleton day={day} />
    </>
  );
}
