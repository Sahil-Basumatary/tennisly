import Link from "next/link";
import { cn } from "@/lib/utils";

type SectionSubnavProps = {
  items: { id: string; label: string; href: string }[];
  activeId?: string;
};

export function SectionSubnav({ items, activeId }: SectionSubnavProps) {
  return (
    <nav
      aria-label="Section"
      className="border-b border-hairline bg-white"
    >
      <div className="mx-auto flex max-w-6xl gap-1 overflow-x-auto px-4 sm:px-6">
        {items.map((item) => {
          const active = item.id === activeId;
          return (
            <Link
              key={item.id}
              href={item.href}
              className={cn(
                "shrink-0 border-b-2 px-3 py-3 font-sans text-[13px] font-semibold transition-colors",
                active
                  ? "border-primary text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
