type LegalPageProps = {
  title: string;
  summary: string;
};

export function LegalPage({ title, summary }: LegalPageProps) {
  return (
    <main id="main-content" className="mx-auto max-w-3xl px-6 py-12">
      <h1 className="mb-4 font-display text-2xl font-semibold text-foreground">
        {title}
      </h1>
      <p className="font-sans text-sm leading-relaxed text-muted-foreground">
        {summary}
      </p>
    </main>
  );
}
