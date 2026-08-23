type PageHeroProps = {
  eyebrow: string;
  title: string;
  description?: string;
};

export function PageHero({ eyebrow, title, description }: PageHeroProps) {
  return (
    <div className="bg-chrome text-chrome-foreground">
      <div className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6 sm:py-10">
        <p className="mb-2 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-uefa-gold">
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
      <div className="h-[3px] bg-wimbledon-green" />
    </div>
  );
}
