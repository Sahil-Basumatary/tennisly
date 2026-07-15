import { AppShell } from "@/components/layout/AppShell";
import { GlobalNav } from "@/components/layout/GlobalNav";
import { ScoresStrip } from "@/components/layout/ScoresStrip";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { getScoresFeed } from "@/services/scores";

export async function SiteChrome({ children }: { children: React.ReactNode }) {
  const feed = await getScoresFeed();

  return (
    <AppShell
      ticker={<ScoresStrip items={feed.items} />}
      nav={<GlobalNav />}
      footer={<SiteFooter />}
    >
      {children}
    </AppShell>
  );
}
