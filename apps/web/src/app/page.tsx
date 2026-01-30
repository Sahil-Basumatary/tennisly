import { ThemeToggle } from "@/components/ui/theme-toggle";

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <span className="font-display text-2xl font-semibold tracking-tight text-foreground">
            Tennisly
          </span>
          <ThemeToggle />
        </div>
      </header>
      <main>
        <section className="relative overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-court-green/10 via-transparent to-royal-purple/10 dark:from-court-green/5 dark:to-championship-gold/10" />
          <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-championship-gold/10 blur-3xl" />
          <div className="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-court-green/10 blur-3xl" />
          <div className="relative mx-auto max-w-6xl px-6 py-24 md:py-32">
            <div className="absolute left-1/2 top-1/2 -z-10 h-px w-[120%] -translate-x-1/2 -translate-y-1/2 -rotate-45 bg-gradient-to-r from-transparent via-border to-transparent opacity-50" />
            <div className="mx-auto max-w-3xl text-center">
              <p className="mb-4 font-serif text-lg text-championship-gold">
                Design v1
              </p>
              <h1 className="mb-6 font-display text-4xl font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
                Championship
                <br />
                <span className="text-primary">Tennis Platform</span>
              </h1>
              <p className="mb-8 font-serif text-xl text-muted-foreground md:text-2xl">
                live visualisation and data analysis for tennis matches
              </p>
              <p className="mx-auto mb-10 max-w-xl font-sans text-base leading-relaxed text-muted-foreground">
                wimbledon legends
              </p>
              <div className="flex flex-col justify-center gap-4 sm:flex-row">
                <button className="rounded-full bg-primary px-8 py-3 font-sans font-medium text-primary-foreground transition-all hover:bg-primary/90 hover:shadow-lg">
                  Get Started
                </button>
                <button className="rounded-full border-2 border-foreground bg-transparent px-8 py-3 font-sans font-medium text-foreground transition-all hover:bg-foreground hover:text-background">
                  View Components
                </button>
              </div>
            </div>
          </div>
        </section>
        <section className="border-t border-border bg-muted/30 py-20">
          <div className="mx-auto max-w-6xl px-6">
            <div className="mb-12 text-center">
              <h2 className="mb-3 font-display text-3xl font-semibold text-foreground">
                Typography System
              </h2>
              <p className="font-sans text-muted-foreground">
                Three carefully selected typefaces for hierarchy and elegance
              </p>
            </div>
            <div className="grid gap-6 md:grid-cols-3">
              <div className="rounded-lg border border-border bg-card p-6 shadow-sm transition-shadow hover:shadow-md">
                <div className="mb-4 inline-block rounded bg-primary/10 px-3 py-1 font-sans text-xs font-medium uppercase tracking-wider text-primary">
                  Headlines
                </div>
                <h3 className="mb-2 font-display text-2xl font-bold text-card-foreground">
                  Playfair Display
                </h3>
                <p className="mb-4 font-sans text-sm text-muted-foreground">
                  Elegant serif for hero text and section headers
                </p>
                <div className="space-y-2 border-t border-border pt-4 font-display text-card-foreground">
                  <p className="text-lg font-normal">Regular 400</p>
                  <p className="text-lg font-medium">Medium 500</p>
                  <p className="text-lg font-semibold">Semibold 600</p>
                  <p className="text-lg font-bold">Bold 700</p>
                </div>
              </div>
              <div className="rounded-lg border border-border bg-card p-6 shadow-sm transition-shadow hover:shadow-md">
                <div className="mb-4 inline-block rounded bg-secondary/10 px-3 py-1 font-sans text-xs font-medium uppercase tracking-wider text-secondary">
                  Accents
                </div>
                <h3 className="mb-2 font-serif text-2xl font-semibold text-card-foreground">
                  Cormorant Garamond
                </h3>
                <p className="mb-4 font-sans text-sm text-muted-foreground">
                  Refined serif for taglines and quotations
                </p>
                <div className="space-y-2 border-t border-border pt-4 font-serif text-card-foreground">
                  <p className="text-lg font-normal">Regular 400</p>
                  <p className="text-lg font-medium">Medium 500</p>
                  <p className="text-lg font-semibold">Semibold 600</p>
                </div>
              </div>
              <div className="rounded-lg border border-border bg-card p-6 shadow-sm transition-shadow hover:shadow-md">
                <div className="mb-4 inline-block rounded bg-accent/10 px-3 py-1 font-sans text-xs font-medium uppercase tracking-wider text-accent">
                  Body
                </div>
                <h3 className="mb-2 font-sans text-2xl font-bold text-card-foreground">
                  Lato
                </h3>
                <p className="mb-4 font-sans text-sm text-muted-foreground">
                  Clean sans-serif for body text and UI elements
                </p>
                <div className="space-y-2 border-t border-border pt-4 font-sans text-card-foreground">
                  <p className="text-lg font-light">Light 300</p>
                  <p className="text-lg font-normal">Regular 400</p>
                  <p className="text-lg font-bold">Bold 700</p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
