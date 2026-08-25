import { CourtReplay2D } from "@/components/court/CourtReplay2D";
import { cn } from "@/lib/utils";

type CourtTopDownFallbackProps = {
  homeName: string;
  awayName: string;
  className?: string;
};

/** @deprecated Prefer CourtReplay2D. Kept so older imports still render a live 2D court. */
export function CourtTopDownFallback({
  homeName,
  awayName,
  className,
}: CourtTopDownFallbackProps) {
  return (
    <CourtReplay2D
      homeName={homeName}
      awayName={awayName}
      className={cn("h-full min-h-[280px]", className)}
    />
  );
}
