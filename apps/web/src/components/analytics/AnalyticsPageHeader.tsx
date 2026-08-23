import type { ReactNode } from "react";

type AnalyticsPageHeaderProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
};

export function AnalyticsPageHeader({
  eyebrow = "Analytics",
  title,
  description,
  actions,
}: AnalyticsPageHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p className="mb-1 font-data text-[11px] font-bold uppercase tracking-[0.16em] text-uefa-gold">
          {eyebrow}
        </p>
        <h1 className="font-display text-2xl font-bold uppercase tracking-tight text-foreground sm:text-3xl">{title}</h1>
        {description ? (
          <p className="mt-2 max-w-2xl font-sans text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}
