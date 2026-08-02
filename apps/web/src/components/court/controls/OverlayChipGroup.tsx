"use client";

import { cn } from "@/lib/utils";

export type OverlayChipOption<T extends string> = {
  key: T;
  label: string;
};

type OverlayChipGroupProps<T extends string> = {
  label: string;
  options: readonly OverlayChipOption<T>[];
  values: Record<T, boolean>;
  onToggle: (key: T) => void;
  size?: "md" | "sm";
  tone?: "light" | "dark";
  hideLabel?: boolean;
  className?: string;
};

/**
 * Broadcast-graphics style overlay toggles: chip with a live indicator dot,
 * matching how slam-tracker panels flag active data layers.
 */
export function OverlayChipGroup<T extends string>({
  label,
  options,
  values,
  onToggle,
  size = "md",
  tone = "light",
  hideLabel = false,
  className,
}: OverlayChipGroupProps<T>) {
  const dark = tone === "dark";
  return (
    <div className={className}>
      <p
        className={cn(
          "mb-1.5 font-sans text-[10px] font-semibold uppercase tracking-[0.16em]",
          dark ? "text-white/55" : "text-muted-foreground",
          hideLabel && "sr-only",
        )}
      >
        {label}
      </p>
      <div role="group" aria-label={label} className="flex flex-wrap gap-1.5">
        {options.map(({ key, label: chipLabel }) => {
          const on = values[key];
          return (
            <button
              key={key}
              type="button"
              onClick={() => onToggle(key)}
              aria-pressed={on}
              className={cn(
                "flex items-center gap-1.5 border font-sans font-semibold uppercase tracking-wide transition-colors",
                size === "md" ? "px-3 py-1.5 text-[11px]" : "px-2.5 py-1 text-[10px]",
                on
                  ? dark
                    ? "border-white/70 bg-white/15 text-white"
                    : "border-foreground bg-white text-foreground"
                  : dark
                    ? "border-white/20 bg-black/25 text-white/55 hover:border-white/45 hover:text-white/85"
                    : "border-hairline bg-white text-muted-foreground hover:border-foreground hover:text-foreground",
              )}
            >
              <span
                aria-hidden
                className={cn(
                  "h-1.5 w-1.5 rounded-full transition-colors",
                  on ? "bg-primary" : dark ? "bg-white/30" : "bg-muted-foreground/40",
                )}
              />
              {chipLabel}
            </button>
          );
        })}
      </div>
    </div>
  );
}
