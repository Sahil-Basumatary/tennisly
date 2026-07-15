import Link from "next/link";

export function SkipToContent() {
  return (
    <Link
      href="#main-content"
      className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:bg-primary focus:px-4 focus:py-2 focus:font-sans focus:text-sm focus:font-semibold focus:text-primary-foreground focus:outline-none"
    >
      Skip to main content
    </Link>
  );
}
