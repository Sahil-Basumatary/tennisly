import Link from "next/link";
import { cn } from "@/lib/utils";

type SectionSubnavProps = {
  items: { id: string; label: string; href: string }[];
  activeId?: string;
};

export function SectionSubnav({ items, activeId }: SectionSubnavProps) {
  return (
    <nav aria-label="Section" className="border-b border-hairline bg-[#f4f1ea]">
      <div className="mx-auto flex max-w-[1400px] gap-1 overflow-x-auto px-4 sm:px-6">
        {items.map((item) => {
          const active = item.id === activeId;
          return (
            <Link
              key={item.id}
              href={item.href}
              className={cn(
                "shrink-0 border-b-2 px-3 py-3 font-data text-[12px] font-bold uppercase tracking-[0.08em] transition-colors",
                active
                  ? "border-wimbledon-green text-chrome"
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
