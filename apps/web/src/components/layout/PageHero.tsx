import { PlayerName } from "@/components/player/PlayerName";

type PageHeroProps = {
  eyebrow: string;
  title: string;
  description?: string;
  portraitUrl?: string | null;
};

export function PageHero({ eyebrow, title, description, portraitUrl }: PageHeroProps) {
  return (
    <div className="bg-chrome text-chrome-foreground">
      <div className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6 sm:py-10">
        <div className="flex items-center gap-5">
          {portraitUrl !== undefined ? (
            <PlayerName name={title} photoUrl={portraitUrl} size="xl" tone="dark" hideName />
          ) : null}
          <div className="min-w-0">
            <p className="mb-2 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-white/70">
              {eyebrow}
            </p>
            <h1 className="font-display text-[28px] font-bold uppercase leading-[1.05] tracking-tight sm:text-[36px]">
              {title}
            </h1>
            {description ? (
              <p className="mt-3 max-w-3xl font-sans text-[15px] leading-relaxed text-white/80">
                {description}
              </p>
            ) : null}
          </div>
        </div>
      </div>
      <div className="h-[3px] bg-court-green" />
    </div>
  );
}
