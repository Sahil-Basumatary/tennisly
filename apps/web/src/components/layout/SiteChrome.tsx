import { GlobalNav } from "@/components/layout/GlobalNav";
import { ScoresStrip } from "@/components/layout/ScoresStrip";
import { getScoresFeed } from "@/services/scores";

export async function SiteChrome({ children }: { children: React.ReactNode }) {
  const feed = await getScoresFeed();

  return (
    <div className="min-h-screen bg-background">
      <ScoresStrip items={feed.items} />
      <GlobalNav />
      {children}
    </div>
  );
}
