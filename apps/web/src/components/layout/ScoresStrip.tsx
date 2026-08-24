"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { ChevronLeftIcon, ChevronRightIcon } from "@/components/ui/brandIcons";
import type { ScoreCard, ScoresFeed } from "@/types/scores";
import { PlayerName } from "@/components/player/PlayerName";
import { cn } from "@/lib/utils";

type ScoresStripProps = {
  items?: ScoreCard[];
};

function statusLabel(card: ScoreCard) {
  if (card.status === "live") return "LIVE";
  if (card.status === "final") return "FINAL";
  return card.startLabel ?? "SOON";
}

function ScoreCardView({ card }: { card: ScoreCard }) {
  return (
    <Link
      href={card.href}
      className="flex h-[65px] min-w-[248px] shrink-0 flex-col justify-center border-r border-white/10 bg-ticker px-3 text-ticker-foreground transition-colors hover:bg-white/5"
    >
      <div className="mb-1 flex items-center justify-between gap-2">
        <span className="font-data text-[10px] font-bold uppercase tracking-wide text-white/55">
          {card.tournament} · {card.round}
        </span>
        <span
          className={cn(
            "font-data text-[10px] font-bold uppercase tracking-wide",
            card.status === "live" ? "text-live" : "text-white/55",
          )}
        >
          {statusLabel(card)}
        </span>
      </div>
      <div className="space-y-0.5">
        {[card.home, card.away].map((side) => (
          <div key={side.shortName} className="flex items-center justify-between gap-3">
            <PlayerName
              name={side.name}
              photoUrl={side.photoUrl}
              size="xs"
              tone="dark"
              bold={side.winner}
              nameClassName={side.winner ? "text-white" : "text-white/90"}
            />
            <span className="font-data text-[13px] font-bold tracking-wide tabular-nums text-white">
              {side.sets.length > 0 ? side.sets.join(" ") : "—"}
            </span>
          </div>
        ))}
      </div>
    </Link>
  );
}

export function ScoresStrip({ items }: ScoresStripProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(false);
  const [cards, setCards] = useState<ScoreCard[]>(items ?? []);

  useEffect(() => {
    if (items) {
      setCards(items);
      return;
    }
    let cancelled = false;
    const load = async () => {
      try {
        const res = await fetch("/api/matches/ticker");
        if (!res.ok) return;
        const feed = (await res.json()) as ScoresFeed;
        if (!cancelled) setCards(feed.items ?? []);
      } catch {
        if (!cancelled) setCards([]);
      }
    };
    void load();
    const timer = window.setInterval(() => void load(), 10_000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [items]);

  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) return;
    const update = () => {
      setCanPrev(el.scrollLeft > 4);
      setCanNext(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
    };
    update();
    el.addEventListener("scroll", update, { passive: true });
    return () => el.removeEventListener("scroll", update);
  }, [cards]);

  const scrollByCards = (dir: -1 | 1) => {
    const el = scrollerRef.current;
    if (!el) return;
    el.scrollBy({ left: dir * 240, behavior: "smooth" });
  };

  return (
    <div
      role="region"
      aria-label="Scores"
      className="border-b border-white/10 bg-ticker text-ticker-foreground"
    >
      <div className="relative mx-auto flex h-ticker max-w-[1400px] items-stretch">
        <div className="flex w-[88px] shrink-0 flex-col justify-center border-r border-white/10 bg-black px-2 sm:w-[124px] sm:px-3">
          <span className="font-data text-[10px] font-bold uppercase tracking-[0.14em] text-white/70 sm:text-[11px]">
            Tennis
          </span>
          <Link
            href="/scores"
            className="font-sans text-[11px] font-semibold text-white hover:underline sm:text-[12px]"
          >
            All Scores
          </Link>
        </div>
        <button
          type="button"
          aria-label="Previous scores"
          disabled={!canPrev}
          onClick={() => scrollByCards(-1)}
          className="flex w-8 shrink-0 items-center justify-center border-r border-white/10 text-white/70 transition-colors hover:bg-white/5 disabled:opacity-30"
        >
          <ChevronLeftIcon className="h-4 w-4" />
        </button>
        <div
          ref={scrollerRef}
          className="flex flex-1 overflow-x-auto scrollbar-none"
          style={{ scrollbarWidth: "none" }}
        >
          {cards.map((card) => (
            <ScoreCardView key={card.id} card={card} />
          ))}
        </div>
        <button
          type="button"
          aria-label="Next scores"
          disabled={!canNext}
          onClick={() => scrollByCards(1)}
          className="flex w-8 shrink-0 items-center justify-center border-l border-white/10 text-white/70 transition-colors hover:bg-white/5 disabled:opacity-30"
        >
          <ChevronRightIcon className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
