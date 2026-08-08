type AnalyticsEmptyStateProps = {
  title?: string;
  message?: string;
};

export function AnalyticsEmptyState({
  title = "No analytics yet",
  message = "No tape-provable matches match these filters. Try another player, surface, or date range.",
}: AnalyticsEmptyStateProps) {
  return (
    <div
      className="border border-hairline bg-white px-6 py-12 text-center"
      role="status"
    >
      <p className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide text-foreground">
        {title}
      </p>
      <p className="mx-auto max-w-md font-sans text-[13px] text-muted-foreground">{message}</p>
    </div>
  );
}
