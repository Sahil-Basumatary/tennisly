"use client";

import { SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import {
  ChevronDownIcon,
  CloseIcon,
  MenuIcon,
  SearchIcon,
} from "@/components/ui/brandIcons";
import { primaryNav, utilityNav, type NavItem } from "@/config/navigation";
import { cn } from "@/lib/utils";

function NavDropdown({ item }: { item: NavItem }) {
  if (!item.children?.length) {
    return (
      <Link
        href={item.href}
        className="inline-flex h-nav items-center px-3 font-sans text-[13px] font-semibold text-chrome-foreground/90 transition-colors hover:bg-white/10 hover:text-white"
      >
        {item.label}
      </Link>
    );
  }

  return (
    <div className="group relative">
      <Link
        href={item.href}
        className="inline-flex h-nav items-center gap-1 px-3 font-sans text-[13px] font-semibold text-chrome-foreground/90 transition-colors hover:bg-white/10 hover:text-white"
      >
        {item.label}
        <ChevronDownIcon className="h-3.5 w-3.5 opacity-70" />
      </Link>
      <div className="invisible absolute left-0 top-full z-50 min-w-[180px] border border-hairline bg-white py-1 opacity-0 shadow-lg transition group-hover:visible group-hover:opacity-100 group-focus-within:visible group-focus-within:opacity-100">
        {item.children.map((child) => (
          <Link
            key={child.id}
            href={child.href}
            className="block px-4 py-2 font-sans text-[13px] text-foreground hover:bg-surface-muted"
          >
            {child.label}
          </Link>
        ))}
      </div>
    </div>
  );
}

function MobileDrawer({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[80] lg:hidden">
      <button
        type="button"
        aria-label="Close menu overlay"
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />
      <aside className="absolute left-0 top-0 flex h-full w-[min(320px,88vw)] flex-col bg-chrome text-chrome-foreground shadow-xl">
        <div className="flex h-nav items-center justify-between border-b border-white/10 px-4">
          <span className="font-display text-base font-semibold">Menu</span>
          <button
            type="button"
            aria-label="Close menu"
            onClick={onClose}
            className="inline-flex h-9 w-9 items-center justify-center hover:bg-white/10"
          >
            <CloseIcon className="h-5 w-5" />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto py-2">
          {[...primaryNav, ...utilityNav].map((item) => (
            <div key={item.id} className="border-b border-white/10">
              <Link
                href={item.href}
                onClick={onClose}
                className="block px-4 py-3 font-sans text-sm font-semibold"
              >
                {item.label}
              </Link>
              {item.children?.map((child) => (
                <Link
                  key={child.id}
                  href={child.href}
                  onClick={onClose}
                  className="block px-6 py-2 font-sans text-sm text-chrome-foreground/75 hover:bg-white/10"
                >
                  {child.label}
                </Link>
              ))}
            </div>
          ))}
        </nav>
      </aside>
    </div>
  );
}

export function GlobalNav() {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <>
      <header className="sticky top-0 z-50 bg-chrome text-chrome-foreground">
        <div className="mx-auto flex h-nav max-w-[1400px] items-center gap-1 px-2 sm:px-4">
          <button
            type="button"
            aria-label="Open menu"
            className="inline-flex h-9 w-9 items-center justify-center hover:bg-white/10 lg:hidden"
            onClick={() => setMenuOpen(true)}
          >
            <MenuIcon className="h-5 w-5" />
          </button>
          <Link
            href="/"
            className="mr-2 inline-flex h-nav items-center px-2 font-display text-[18px] font-bold tracking-tight text-white"
          >
            Tennisly
          </Link>
          <nav className="hidden flex-1 items-center lg:flex">
            {primaryNav.map((item) => (
              <NavDropdown key={item.id} item={item} />
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-1">
            <button
              type="button"
              aria-label="Open search"
              aria-expanded={searchOpen}
              onClick={() => setSearchOpen((value) => !value)}
              className="inline-flex h-9 w-9 items-center justify-center hover:bg-white/10"
            >
              <SearchIcon className="h-5 w-5" />
            </button>
            {searchOpen ? (
              <label className="hidden items-center sm:flex">
                <span className="sr-only">Search</span>
                <input
                  autoFocus
                  type="search"
                  placeholder="Search players, tournaments..."
                  className="h-8 w-[220px] border-0 bg-white/10 px-3 font-sans text-[13px] text-white outline-none placeholder:text-white/50 focus:bg-white/15"
                />
              </label>
            ) : null}
            <SignedOut>
              <Link
                href="/sign-in"
                className={cn(
                  "ml-1 inline-flex h-8 items-center px-3 font-sans text-[12px] font-bold uppercase tracking-wide",
                  "border border-white/30 text-white transition-colors hover:bg-white hover:text-chrome",
                )}
              >
                Sign In
              </Link>
            </SignedOut>
            <SignedIn>
              <div className="ml-2 flex items-center">
                <UserButton
                  afterSignOutUrl="/"
                  appearance={{
                    elements: {
                      avatarBox: "h-8 w-8",
                    },
                  }}
                />
              </div>
            </SignedIn>
          </div>
        </div>
        <div className="border-t border-white/10 bg-[#1f2021]">
          <div className="mx-auto flex h-9 max-w-[1400px] items-center gap-1 overflow-x-auto px-2 sm:px-4">
            {primaryNav.map((item) => {
              const active =
                pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={`sub-${item.id}`}
                  href={item.href}
                  className={cn(
                    "inline-flex h-9 shrink-0 items-center px-3 font-sans text-[12px] font-semibold uppercase tracking-wide transition-colors",
                    active
                      ? "bg-white/10 text-white"
                      : "text-white/70 hover:bg-white/5 hover:text-white",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>
      </header>
      <MobileDrawer open={menuOpen} onClose={() => setMenuOpen(false)} />
    </>
  );
}
