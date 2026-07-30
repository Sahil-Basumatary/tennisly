"use client";

import { cn } from "@/lib/utils";

export type SegmentedOption<T extends string> = {
  id: T;
  label: string;
};

type SegmentedControlProps<T extends string> = {
  label: string;
  options: readonly SegmentedOption<T>[];
  value: T;
  onChange: (id: T) => void;
  size?: "md" | "sm";
  hideLabel?: boolean;
  className?: string;
};

/**
 * Labelled segmented switch — the control language used across the
 * broadcast viz chrome (surface, tour, camera groups).
 */
export function SegmentedControl<T extends string>({
  label,
  options,
  value,
  onChange,
  size = "md",
  hideLabel = false,
  className,
}: SegmentedControlProps<T>) {
  return (
    <div className={className}>
      <p
        className={cn(
          "mb-1.5 font-sans text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground",
          hideLabel && "sr-only",
        )}
      >
        {label}
      </p>
      <div role="group" aria-label={label} className="flex w-max border border-hairline bg-white">
        {options.map((option, index) => {
          const active = option.id === value;
          return (
            <button
              key={option.id}
              type="button"
              onClick={() => onChange(option.id)}
              aria-pressed={active}
              className={cn(
                "font-sans font-semibold uppercase tracking-wide transition-colors",
                size === "md" ? "px-3.5 py-1.5 text-[11px]" : "px-2.5 py-1 text-[10px]",
                index > 0 && "border-l border-hairline",
                active
                  ? "bg-foreground text-background"
                  : "bg-white text-foreground hover:bg-muted",
              )}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
