import { GlobalNav } from "@/components/layout/GlobalNav";
import { ScoresStrip } from "@/components/layout/ScoresStrip";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { getScoresFeed } from "@/services/scores";

export async function SiteChrome({ children }: { children: React.ReactNode }) {
  const feed = await getScoresFeed();

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <ScoresStrip items={feed.items} />
      <GlobalNav />
      <div className="flex-1">{children}</div>
      <SiteFooter />
    </div>
  );
}
