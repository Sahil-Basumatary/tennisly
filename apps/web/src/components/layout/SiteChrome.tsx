import { AppShell } from "@/components/layout/AppShell";
import { GlobalNav } from "@/components/layout/GlobalNav";
import { ScoresStrip } from "@/components/layout/ScoresStrip";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SkipToContent } from "@/components/layout/SkipToContent";

export async function SiteChrome({ children }: { children: React.ReactNode }) {
  return (
    <>
      <SkipToContent />
      <AppShell
        ticker={<ScoresStrip />}
        nav={<GlobalNav />}
        footer={<SiteFooter />}
      >
        {children}
      </AppShell>
    </>
  );
}
