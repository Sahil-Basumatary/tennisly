export default function DashboardPage() {
  return (
    <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
        Account
      </p>
      <h1 className="mb-3 font-display text-2xl font-semibold text-foreground sm:text-3xl">
        Dashboard
      </h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {["Saved matches", "Following", "Preferences"].map((label) => (
          <div
            key={label}
            className="min-h-[140px] border border-hairline bg-white p-5"
          >
            <h2 className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide">
              {label}
            </h2>
            <p className="font-sans text-[13px] text-muted-foreground">
              Slot for member surfaces. Wired in later milestones.
            </p>
          </div>
        ))}
      </div>
    </main>
  );
}
