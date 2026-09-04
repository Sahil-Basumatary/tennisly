"use client";

import { useState } from "react";
import { HEADSHOT_FOCUS } from "@/lib/headshot";
import { playerInitials, publicPlayerName } from "@/lib/player-directory";
import { cn } from "@/lib/utils";

const SIZES = {
  xs: 16,
  sm: 20,
  md: 28,
  lg: 40,
  xl: 88,
} as const;

type PlayerNameProps = {
  name: string;
  photoUrl?: string | null;
  size?: keyof typeof SIZES;
  tone?: "light" | "dark";
  bold?: boolean;
  hideName?: boolean;
  className?: string;
  nameClassName?: string;
};

export function PlayerName({
  name,
  photoUrl,
  size = "sm",
  tone = "light",
  bold,
  hideName,
  className,
  nameClassName,
}: PlayerNameProps) {
  const px = SIZES[size];
  const dark = tone === "dark";
  const label = publicPlayerName(name);
  const [failedPhotoUrl, setFailedPhotoUrl] = useState<string | null>(null);
  const showPhoto = Boolean(photoUrl) && failedPhotoUrl !== photoUrl;
  return (
    <span className={cn("inline-flex min-w-0 max-w-full items-center gap-2", className)}>
      <span
        className={cn(
          "relative shrink-0 overflow-hidden rounded-full",
          dark ? "bg-white/15 ring-1 ring-white/25" : "bg-chrome text-white ring-1 ring-hairline",
        )}
        style={{ width: px, height: px }}
      >
        {showPhoto ? (
          <img
            src={photoUrl ?? ""}
            alt=""
            width={px}
            height={px}
            className={cn("h-full w-full", HEADSHOT_FOCUS)}
            referrerPolicy="no-referrer"
            onError={() => setFailedPhotoUrl(photoUrl ?? null)}
          />
        ) : (
          <span
            className="flex h-full w-full items-center justify-center font-data font-bold leading-none text-white"
            style={{ fontSize: Math.max(8, Math.round(px * 0.36)) }}
          >
            {playerInitials(label)}
          </span>
        )}
      </span>
      {hideName ? (
        <span className="sr-only">{label}</span>
      ) : (
        <span
          className={cn(
            "min-w-0 truncate font-sans leading-tight",
            bold ? "font-bold" : "font-semibold",
            nameClassName,
          )}
        >
          {label}
        </span>
      )}
    </span>
  );
}
