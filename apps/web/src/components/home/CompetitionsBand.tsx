import Link from "next/link";
import { competitionRail } from "@/config/navigation";

export function CompetitionsBand() {
  return (
    <section className="bg-black text-white">
      <div className="mx-auto max-w-[1400px] px-4 py-10 sm:px-6 sm:py-12">
        <p className="mb-2 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-uefa-gold">
          Competitions
        </p>
        <h2 className="mb-6 font-display text-[28px] font-bold uppercase tracking-tight sm:text-[32px]">
          Follow every tour
        </h2>
        <div className="grid gap-px bg-white/10 sm:grid-cols-2 lg:grid-cols-3">
          {competitionRail.map((item) => (
            <Link
              key={item.id}
              href={item.href}
              className="bg-black px-5 py-6 transition-colors hover:bg-white/5"
            >
              <span className="font-data text-[11px] font-bold uppercase tracking-[0.14em] text-uefa-gold">
                Draw
              </span>
              <span className="mt-2 block font-display text-[20px] font-semibold uppercase tracking-tight">
                {item.label}
              </span>
              <span className="mt-3 inline-block font-sans text-[13px] text-white/70">
                Scores, draws and replays →
              </span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
