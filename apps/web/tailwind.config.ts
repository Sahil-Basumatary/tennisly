import type { Config } from "tailwindcss";
import tailwindcssAnimate from "tailwindcss-animate";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        display: ["var(--font-display)", "system-ui", "sans-serif"],
        data: ["var(--font-data)", "Roboto Condensed", "Arial Narrow", "sans-serif"],
      },
      fontSize: {
        xs: ["var(--text-xs)", { lineHeight: "1.25" }],
        sm: ["var(--text-sm)", { lineHeight: "1.35" }],
        base: ["var(--text-base)", { lineHeight: "1.5" }],
        md: ["var(--text-md)", { lineHeight: "1.4" }],
        lg: ["var(--text-lg)", { lineHeight: "1.3" }],
        xl: ["var(--text-xl)", { lineHeight: "1.2" }],
        "2xl": ["var(--text-2xl)", { lineHeight: "1.15" }],
        hero: ["var(--text-hero)", { lineHeight: "1.1", fontWeight: "300" }],
      },
      spacing: {
        nav: "var(--nav-height)",
        header: "var(--header-height)",
        ticker: "var(--ticker-height)",
      },
      colors: {
        "court-green": "hsl(var(--court-green))",
        "court-glow": "hsl(var(--court-glow))",
        "grass-light": "#228B22",
        "grass-dark": "#004225",
        "clay-terracotta": "#C65D3B",
        "roland-clay": "#D2691E",
        "cream-white": "#FFFEF2",
        "deep-navy": "#1A1A2E",
        charcoal: "#2D2D2D",
        "soft-gray": "#F2F4F8",
        chrome: {
          DEFAULT: "hsl(var(--chrome))",
          foreground: "hsl(var(--chrome-foreground))",
        },
        ticker: {
          DEFAULT: "hsl(var(--ticker))",
          foreground: "hsl(var(--ticker-foreground))",
        },
        live: "hsl(var(--live))",
        hairline: "hsl(var(--hairline))",
        "surface-muted": "hsl(var(--surface-muted))",
        "inverse-deep": "hsl(var(--inverse-deep))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 1px)",
        sm: "0",
      },
    },
  },
  plugins: [tailwindcssAnimate],
};

export default config;
