"use client";

import { SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import {
  ChevronDownIcon,
  CloseIcon,
  MenuIcon,
  SearchIcon,
} from "@/components/ui/brandIcons";
import {
  competitionRail,
  primaryNav,
  utilityNav,
  type NavItem,
} from "@/config/navigation";
import { cn } from "@/lib/utils";

function NavDropdown({
  item,
  open,
  active,
  onOpen,
  onClose,
}: {
  item: NavItem;
  open: boolean;
  active: boolean;
  onOpen: () => void;
  onClose: () => void;
}) {
  if (!item.children?.length) {
    return (
      <Link
        href={item.href}
        className={cn(
          "inline-flex h-nav items-center px-3 font-sans text-[13px] font-semibold transition-colors hover:bg-white/10 hover:text-white",
          active ? "bg-white/10 text-white" : "text-chrome-foreground/90",
        )}
      >
        {item.label}
      </Link>
    );
  }

  return (
    <div
      className="relative"
      onMouseEnter={onOpen}
      onMouseLeave={onClose}
      onFocus={onOpen}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
          onClose();
        }
      }}
    >
      <Link
        href={item.href}
        onClick={onClose}
        aria-expanded={open}
        aria-haspopup="menu"
        className={cn(
          "inline-flex h-nav items-center gap-1 px-3 font-sans text-[13px] font-semibold transition-colors hover:bg-white/10 hover:text-white",
          active || open ? "bg-white/10 text-white" : "text-chrome-foreground/90",
        )}
      >
        {item.label}
        <ChevronDownIcon className="h-3.5 w-3.5 opacity-70" />
      </Link>
      <div
        role="menu"
        className={cn(
          "absolute left-0 top-full z-50 min-w-[200px] border border-hairline bg-white py-1 shadow-lg transition",
          open ? "visible opacity-100" : "invisible pointer-events-none opacity-0",
        )}
      >
        {item.children.map((child) => (
          <Link
            key={child.id}
            href={child.href}
            role="menuitem"
            onClick={onClose}
            className="block px-4 py-2 font-sans text-[13px] text-foreground hover:bg-surface-muted"
          >
            {child.label}
          </Link>
        ))}
      </div>
    </div>
  );
}

function railItemActive(href: string, pathname: string, query: string) {
  const url = new URL(href, "https://tennisly.tv");
  if (pathname !== url.pathname) return false;
  const want = url.searchParams.toString();
  if (!want) return query === "";
  return query === want;
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
          <p className="px-4 pb-1 pt-3 font-data text-[10px] font-bold uppercase tracking-[0.14em] text-white/50">
            Competitions
          </p>
          {competitionRail.map((item) => (
            <Link
              key={item.id}
              href={item.href}
              onClick={onClose}
              className="block px-4 py-2 font-sans text-sm text-chrome-foreground/90 hover:bg-white/10"
            >
              {item.label}
            </Link>
          ))}
          {[...primaryNav, ...utilityNav].map((item) => (
            <div key={item.id} className="border-t border-white/10">
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
  const searchParams = useSearchParams();
  const query = searchParams.toString();
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [openDropdownId, setOpenDropdownId] = useState<string | null>(null);
  const [navigatedFrom, setNavigatedFrom] = useState(pathname);

  if (pathname !== navigatedFrom) {
    setNavigatedFrom(pathname);
    setOpenDropdownId(null);
    setMenuOpen(false);
  }

  useEffect(() => {
    if (!openDropdownId) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpenDropdownId(null);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [openDropdownId]);

  return (
    <>
      <header className="bg-chrome text-chrome-foreground">
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
            className="mr-3 inline-flex h-nav items-center px-2 font-display text-[20px] font-bold tracking-tight text-white"
          >
            Tennisly
          </Link>
          <nav aria-label="Primary" className="hidden flex-1 items-center lg:flex">
            {primaryNav.map((item) => {
              const active =
                pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <NavDropdown
                  key={item.id}
                  item={item}
                  open={openDropdownId === item.id}
                  active={active}
                  onOpen={() => setOpenDropdownId(item.id)}
                  onClose={() => setOpenDropdownId(null)}
                />
              );
            })}
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
              <form action="/players" method="get" className="hidden items-center sm:flex">
                <label className="sr-only" htmlFor="nav-search">
                  Search players
                </label>
                <input
                  id="nav-search"
                  autoFocus
                  type="search"
                  name="q"
                  placeholder="Search players"
                  className="h-8 w-[220px] border-0 bg-white/10 px-3 font-sans text-[13px] text-white outline-none placeholder:text-white/50 focus:bg-white/15"
                />
              </form>
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
        <div className="h-[3px] bg-court-green" />
        <nav
          aria-label="Competitions"
          className="bg-black"
        >
          <div className="mx-auto flex h-10 max-w-[1400px] items-stretch gap-0 overflow-x-auto px-2 sm:px-4">
            {competitionRail.map((item) => {
              const active = railItemActive(item.href, pathname, query);
              return (
                <Link
                  key={item.id}
                  href={item.href}
                  className={cn(
                    "inline-flex shrink-0 items-center border-b-2 px-3 font-data text-[11px] font-bold uppercase tracking-[0.12em] transition-colors",
                    active
                      ? "border-court-glow text-court-glow"
                      : "border-transparent text-white/75 hover:text-white",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        </nav>
      </header>
      <MobileDrawer open={menuOpen} onClose={() => setMenuOpen(false)} />
    </>
  );
}
