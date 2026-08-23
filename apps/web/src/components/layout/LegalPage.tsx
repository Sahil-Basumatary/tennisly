import type { ReactNode } from "react";
import { PageHero } from "@/components/layout/PageHero";

type LegalPageProps = {
  eyebrow?: string;
  title: string;
  summary: string;
  children?: ReactNode;
};

export function LegalPage({
  eyebrow = "Tennisly",
  title,
  summary,
  children,
}: LegalPageProps) {
  return (
    <>
      <PageHero eyebrow={eyebrow} title={title} description={summary} />
      <main id="main-content" className="mx-auto max-w-[800px] px-4 py-10 sm:px-6">
        {children ? (
          <div className="space-y-4 font-sans text-[15px] leading-relaxed text-foreground">
            {children}
          </div>
        ) : (
          <p className="font-sans text-[15px] leading-relaxed text-muted-foreground">
            {summary}
          </p>
        )}
      </main>
    </>
  );
}
