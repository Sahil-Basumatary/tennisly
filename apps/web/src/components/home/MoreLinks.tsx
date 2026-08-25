import Link from "next/link";
import type { HomeMoreItem } from "@/services/home";
import { cn } from "@/lib/utils";

type MoreLinksProps = {
  title: string;
  items: HomeMoreItem[];
  footer?: { href: string; label: string };
  className?: string;
};

export function MoreLinks({ title, items, footer, className }: MoreLinksProps) {
  if (items.length === 0 && !footer) return null;
  return (
    <div className={cn(className)}>
      {items.length > 0 ? (
        <>
          <h3 className="mb-3 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-muted-foreground">
            {title}
          </h3>
          <ul className="border border-hairline">
            {items.map((item) => (
              <li key={item.id} className="border-b border-hairline last:border-b-0">
                <Link
                  href={item.href}
                  className="flex items-baseline justify-between gap-4 px-4 py-3 hover:bg-surface-muted"
                >
                  <span className="min-w-0">
                    <span className="block font-data text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
                      {item.eyebrow}
                    </span>
                    <span className="mt-0.5 block truncate font-sans text-[15px] font-semibold text-foreground">
                      {item.title}
                    </span>
                  </span>
                  <span className="shrink-0 font-data text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
                    {item.meta}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </>
      ) : null}
      {footer ? (
        <p className="mt-3">
          <Link href={footer.href} className="font-sans text-[13px] font-semibold text-chrome hover:underline">
            {footer.label}
          </Link>
        </p>
      ) : null}
    </div>
  );
}
