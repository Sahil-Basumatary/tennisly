"use client";

import { useEffect, useRef, useState } from "react";
import {
  ChevronLeftIcon,
  ChevronRightIcon,
} from "@/components/ui/brandIcons";
import { StoryCard } from "@/components/home/StoryCard";
import type { HomeStory } from "@/services/home";

type EditorialCarouselProps = {
  title: string;
  stories: HomeStory[];
};

export function EditorialCarousel({ title, stories }: EditorialCarouselProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(false);

  const sync = () => {
    const el = scrollerRef.current;
    if (!el) return;
    setCanPrev(el.scrollLeft > 4);
    setCanNext(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
  };

  useEffect(() => {
    sync();
    const el = scrollerRef.current;
    if (!el) return;
    el.addEventListener("scroll", sync, { passive: true });
    window.addEventListener("resize", sync);
    return () => {
      el.removeEventListener("scroll", sync);
      window.removeEventListener("resize", sync);
    };
  }, [stories]);

  const scrollBy = (dir: -1 | 1) => {
    scrollerRef.current?.scrollBy({ left: dir * 272, behavior: "smooth" });
  };

  return (
    <section className="bg-white py-12 md:py-16">
      <div className="mx-auto max-w-[1400px] px-6 md:px-10">
        <div className="mb-8 flex items-end justify-between gap-4">
          <h2 className="font-display text-[32px] font-bold leading-none tracking-tight text-foreground md:text-[40px]">
            {title}
          </h2>
          <div className="hidden items-center gap-2 sm:flex">
            <button
              type="button"
              aria-label="Previous stories"
              disabled={!canPrev}
              onClick={() => scrollBy(-1)}
              className="inline-flex h-9 w-9 items-center justify-center border border-hairline text-foreground transition-colors hover:bg-surface-muted disabled:opacity-30"
            >
              <ChevronLeftIcon className="h-4 w-4" />
            </button>
            <button
              type="button"
              aria-label="Next stories"
              disabled={!canNext}
              onClick={() => scrollBy(1)}
              className="inline-flex h-9 w-9 items-center justify-center border border-hairline text-foreground transition-colors hover:bg-surface-muted disabled:opacity-30"
            >
              <ChevronRightIcon className="h-4 w-4" />
            </button>
          </div>
        </div>
        <div
          ref={scrollerRef}
          className="flex gap-8 overflow-x-auto pb-2"
          style={{ scrollbarWidth: "none" }}
        >
          {stories.map((story) => (
            <StoryCard key={story.id} story={story} />
          ))}
        </div>
      </div>
    </section>
  );
}
