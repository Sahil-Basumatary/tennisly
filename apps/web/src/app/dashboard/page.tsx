export default function DashboardPage() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-10">
      <p className="mb-2 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
        Account
      </p>
      <h1 className="mb-3 font-display text-2xl font-semibold text-foreground">
        Dashboard
      </h1>
      <p className="max-w-2xl font-sans text-sm text-muted-foreground">
        Signed-in home for saved matches and preferences. Full account surfaces
        land in later milestones.
      </p>
    </main>
  );
}
