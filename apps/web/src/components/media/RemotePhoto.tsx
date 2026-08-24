"use client";

import { useEffect, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

type RemotePhotoProps = {
  src: string | null | undefined;
  alt: string;
  className?: string;
  fallback: ReactNode;
};

export function RemotePhoto({ src, alt, className, fallback }: RemotePhotoProps) {
  const [broken, setBroken] = useState(false);
  useEffect(() => {
    setBroken(false);
  }, [src]);
  if (!src || broken) return fallback;
  return (
    <img
      src={src}
      alt={alt}
      className={cn("absolute inset-0 h-full w-full", className)}
      referrerPolicy="no-referrer"
      onError={() => setBroken(true)}
    />
  );
}
