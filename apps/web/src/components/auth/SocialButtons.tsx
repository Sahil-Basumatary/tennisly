"use client";

import {
  AppleGlyphIcon,
  FacebookIcon,
  GoogleGlyphIcon,
} from "@/components/ui/brandIcons";
import { cn } from "@/lib/utils";

type Strategy = "oauth_google" | "oauth_apple" | "oauth_facebook";

type SocialButtonsProps = {
  onSelect: (strategy: Strategy) => void;
  disabled?: boolean;
};

const providers: {
  strategy: Strategy;
  label: string;
  icon: React.ReactNode;
}[] = [
  {
    strategy: "oauth_google",
    label: "Google",
    icon: <GoogleGlyphIcon className="h-5 w-5" />,
  },
  {
    strategy: "oauth_apple",
    label: "Apple",
    icon: <AppleGlyphIcon className="h-5 w-5 text-[#121619]" />,
  },
  {
    strategy: "oauth_facebook",
    label: "Facebook",
    icon: <FacebookIcon className="h-5 w-5 text-[#1877F2]" />,
  },
];

export function SocialButtons({ onSelect, disabled }: SocialButtonsProps) {
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
      {providers.map((p) => (
        <button
          key={p.strategy}
          type="button"
          disabled={disabled}
          onClick={() => onSelect(p.strategy)}
          className={cn(
            "inline-flex h-11 items-center justify-center gap-2 rounded-lg border border-[#b6bdc5] bg-[#f2f4f8] px-3 font-sans text-[14px] font-medium text-[#121619] transition-colors hover:bg-[#e4e9ef] disabled:opacity-60",
          )}
        >
          {p.icon}
          <span>{p.label}</span>
        </button>
      ))}
    </div>
  );
}
