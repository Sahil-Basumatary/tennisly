import { notFound } from "next/navigation";
import { MatchCentreSkeleton } from "@/components/scaffolds/MatchCentreSkeleton";
import { getMatchCentre } from "@/services/scaffolds";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function MatchCentrePage({ params }: PageProps) {
  const { id } = await params;
  const match = await getMatchCentre(id);
  if (!match) notFound();

  return <MatchCentreSkeleton match={match} />;
}
