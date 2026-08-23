import Link from "next/link";
import type { TourPulse } from "@/services/home";
import { cn } from "@/lib/utils";

export function TourPulse({ pulse }: { pulse: TourPulse }) {
  const live = pulse.featured.status === "live";
  return (
    <section className="bg-chrome text-chrome-foreground">
      <div className="mx-auto grid max-w-[1400px] gap-px bg-white/10 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <Link
          href={pulse.featured.href}
          className="bg-chrome px-4 py-8 sm:px-6 sm:py-10"
        >
          <p className="mb-2 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-white/65">
            Tour pulse
          </p>
          <p
            className={cn(
              "mb-3 font-data text-[11px] font-bold uppercase tracking-[0.16em]",
              live ? "text-live" : "text-white/65",
            )}
          >
            {live ? "Live now" : pulse.featured.status === "final" ? "Latest result" : "Next on court"}
          </p>
          <h2 className="max-w-[18ch] font-display text-[28px] font-bold uppercase leading-[1.05] tracking-tight sm:text-[32px]">
            {pulse.featured.title}
          </h2>
          <p className="mt-2 font-sans text-[14px] text-white/75">{pulse.featured.meta}</p>
          <span className="mt-6 inline-block font-sans text-[13px] font-semibold text-white">
            Open Match Centre →
          </span>
        </Link>
        {pulse.circuits.length > 0 ? (
          <div className="bg-chrome px-4 py-8 sm:px-6 sm:py-10">
            <p className="mb-4 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-white/65">
              Active circuits
            </p>
            <ul>
              {pulse.circuits.map((circuit) => (
                <li key={circuit.id} className="border-t border-white/10 first:border-t-0">
                  <Link
                    href={circuit.href}
                    className="flex items-center justify-between gap-4 py-3 transition-colors hover:text-white"
                  >
                    <span>
                      <span className="block font-sans text-[15px] font-semibold">{circuit.label}</span>
                      <span className="block font-sans text-[12px] text-white/65">
                        {circuit.sample ?? "On the calendar"}
                      </span>
                    </span>
                    <span className="shrink-0 font-data text-[11px] font-bold uppercase tracking-wide text-white/70">
                      {circuit.liveCount > 0
                        ? `${circuit.liveCount} live`
                        : circuit.upcomingCount > 0
                          ? `${circuit.upcomingCount} upcoming`
                          : "Results"}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
      <div className="h-[3px] bg-court-green" />
    </section>
  );
}
