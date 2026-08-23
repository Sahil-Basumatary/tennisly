type AnalyticsErrorPanelProps = {
  message?: string;
};

export function AnalyticsErrorPanel({
  message = "Analytics is temporarily unavailable. Try again shortly.",
}: AnalyticsErrorPanelProps) {
  return (
    <div className="border border-destructive/30 bg-white px-6 py-8" role="alert">
      <p className="mb-1 font-sans text-[14px] font-bold uppercase tracking-wide text-destructive">
        Could not load analytics
      </p>
      <p className="font-sans text-[13px] text-muted-foreground">{message}</p>
    </div>
  );
}
