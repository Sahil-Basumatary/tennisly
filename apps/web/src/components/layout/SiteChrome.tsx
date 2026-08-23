import { Suspense } from "react";
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
        nav={
          <div className="sticky top-0 z-50">
            <ScoresStrip />
            <Suspense fallback={<div className="h-nav bg-chrome" />}>
              <GlobalNav />
            </Suspense>
          </div>
        }
        footer={<SiteFooter />}
      >
        {children}
      </AppShell>
    </>
  );
}
