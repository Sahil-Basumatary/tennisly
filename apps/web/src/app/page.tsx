import { ThemeToggle } from "@/components/ui/theme-toggle";

const colors = [
  { name: "Court Green", hex: "#006633", variable: "court-green" },
  { name: "Royal Purple", hex: "#540082", variable: "royal-purple" },
  { name: "Brand Gold", hex: "#816c3c", variable: "championship-gold" },
  { name: "Chrome", hex: "#2b2c2d", variable: "chrome" },
  { name: "Ticker", hex: "#f1f2f3", variable: "ticker" },
  { name: "Surface Muted", hex: "#f2f4f8", variable: "soft-gray" },
  { name: "Inverse Deep", hex: "#0b2917", variable: "inverse-deep" },
  { name: "Inverse Purple", hex: "#240330", variable: "inverse-purple" },
  { name: "Destructive", hex: "#da1e28", variable: "destructive" },
];

export default function Home() {
  return (
    <main>
      <section className="relative overflow-hidden border-b border-hairline">
        <div className="mx-auto max-w-6xl px-6 py-20 md:py-28">
          <div className="mb-4 flex items-center justify-between gap-4">
            <p className="font-sans text-sm font-semibold uppercase tracking-[0.18em] text-primary">
              Typography foundation
            </p>
            <ThemeToggle />
          </div>
          <h1 className="mb-5 font-display text-hero font-light text-foreground">
            Championship
            <br />
            tennis platform
          </h1>
          <p className="mb-8 max-w-2xl font-sans text-md text-muted-foreground">
            Global nav and scores strip are live above. Homepage editorial
            surface lands in the next milestone.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <a
              href="/scores"
              className="bg-primary px-8 py-3 text-center font-sans text-sm font-semibold uppercase tracking-wide text-primary-foreground transition-colors hover:bg-primary/90"
            >
              View Scores
            </a>
            <a
              href="/sign-in"
              className="border border-foreground bg-transparent px-8 py-3 text-center font-sans text-sm font-semibold uppercase tracking-wide text-foreground transition-colors hover:bg-foreground hover:text-background"
            >
              Sign In
            </a>
          </div>
        </div>
      </section>
      <section className="border-b border-hairline bg-surface-muted py-16">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mb-10">
            <h2 className="mb-2 font-display text-xl font-semibold text-foreground">
              Type stack
            </h2>
            <p className="font-sans text-sm text-muted-foreground">
              Montserrat for UI. Roboto Condensed for scores and dense data.
            </p>
          </div>
          <div className="grid gap-6 md:grid-cols-2">
            <div className="border border-hairline bg-card p-6">
              <p className="mb-3 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                UI · Headlines
              </p>
              <h3 className="mb-2 font-display text-2xl font-semibold text-card-foreground">
                Montserrat
              </h3>
              <div className="space-y-2 border-t border-hairline pt-4 font-display text-card-foreground">
                <p className="text-lg font-light">Light 300</p>
                <p className="text-lg font-normal">Regular 400</p>
                <p className="text-lg font-semibold">Semibold 600</p>
                <p className="text-lg font-bold">Bold 700</p>
              </div>
            </div>
            <div className="border border-hairline bg-card p-6">
              <p className="mb-3 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-secondary">
                Scores · Data
              </p>
              <h3 className="mb-2 font-data text-2xl font-bold uppercase tracking-wide text-card-foreground">
                Roboto Condensed
              </h3>
              <div className="space-y-2 border-t border-hairline pt-4 font-data text-card-foreground">
                <p className="text-lg font-light uppercase tracking-wide">
                  Light 300 · 6-4 7-5
                </p>
                <p className="text-lg font-bold uppercase tracking-wide">
                  Bold 700 · LIVE
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section className="py-16">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mb-10">
            <h2 className="mb-2 font-display text-xl font-semibold text-foreground">
              Color tokens
            </h2>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {colors.map((color) => (
              <div
                key={color.variable}
                className="border border-hairline bg-card p-4"
              >
                <div
                  className="mb-3 h-16 w-full ring-1 ring-black/5"
                  style={{ backgroundColor: color.hex }}
                />
                <p className="font-sans text-sm font-medium text-card-foreground">
                  {color.name}
                </p>
                <p className="font-data text-xs uppercase tracking-wide text-muted-foreground">
                  {color.hex}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
