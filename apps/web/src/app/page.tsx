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
                Design System Showcase
              </p>
              <h1 className="mb-6 font-display text-4xl font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
                Championship-Grade
                <br />
                <span className="text-primary">Tennis Platform</span>
              </h1>
              <p className="mb-8 font-serif text-xl text-muted-foreground md:text-2xl">
                Where tradition meets innovation on the digital court
              </p>
              <p className="mx-auto mb-10 max-w-xl font-sans text-base leading-relaxed text-muted-foreground">
                A premium design system inspired by the elegance of Wimbledon,
                combining classic championship aesthetics with modern interface
                patterns.
              </p>
              <div className="flex flex-col justify-center gap-4 sm:flex-row">
                <button className="rounded-full bg-primary px-8 py-3 font-sans font-medium text-primary-foreground transition-all hover:bg-primary/90 hover:shadow-lg">
                  Get Started
                </button>
                <button className="rounded-full border-2 border-secondary bg-transparent px-8 py-3 font-sans font-medium text-secondary transition-all hover:bg-secondary hover:text-secondary-foreground">
                  View Components
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
