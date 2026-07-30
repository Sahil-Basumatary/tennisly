import { cn } from "@/lib/utils";

type CourtTopDownFallbackProps = {
  homeName: string;
  awayName: string;
  className?: string;
};

/**
 * Static 2D top-down court when WebGL is unavailable.
 * Keeps match-centre layout intact without Babylon.
 */
export function CourtTopDownFallback({
  homeName,
  awayName,
  className,
}: CourtTopDownFallbackProps) {
  return (
    <div
      className={cn(
        "relative flex min-h-[280px] w-full items-center justify-center bg-[linear-gradient(180deg,#0b5c2e_0%,#087038_45%,#0b5c2e_100%)] p-4",
        className,
      )}
      role="img"
      aria-label={`Top-down tennis court diagram. ${homeName} versus ${awayName}.`}
    >
      <svg
        viewBox="0 0 200 360"
        className="h-full max-h-[360px] w-full max-w-[220px]"
        aria-hidden
      >
        <rect x="20" y="20" width="160" height="320" fill="#0d6b36" stroke="#f5f5f5" strokeWidth="2" />
        <rect x="36" y="20" width="128" height="320" fill="none" stroke="#f5f5f5" strokeWidth="1.2" />
        <line x1="20" y1="180" x2="180" y2="180" stroke="#111" strokeWidth="2.5" />
        <line x1="100" y1="20" x2="100" y2="340" stroke="#f5f5f5" strokeWidth="1" opacity="0.7" />
        <line x1="36" y1="100" x2="164" y2="100" stroke="#f5f5f5" strokeWidth="1.2" />
        <line x1="36" y1="260" x2="164" y2="260" stroke="#f5f5f5" strokeWidth="1.2" />
        <line x1="100" y1="100" x2="100" y2="260" stroke="#f5f5f5" strokeWidth="1.2" />
        <circle cx="100" cy="280" r="5" fill="#f4f4f4" />
        <circle cx="100" cy="80" r="5" fill="#268cff" />
        <text x="100" y="310" textAnchor="middle" fill="#fff" fontSize="9" fontFamily="sans-serif">
          {homeName.split(" ").slice(-1)[0]}
        </text>
        <text x="100" y="58" textAnchor="middle" fill="#fff" fontSize="9" fontFamily="sans-serif">
          {awayName.split(" ").slice(-1)[0]}
        </text>
      </svg>
      <p className="absolute bottom-2 left-1/2 -translate-x-1/2 font-sans text-[10px] font-semibold uppercase tracking-wide text-white/75">
        2D court · WebGL unavailable
      </p>
    </div>
  );
}
