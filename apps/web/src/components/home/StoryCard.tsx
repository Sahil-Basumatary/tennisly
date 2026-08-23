import Image from "next/image";
import Link from "next/link";
import { playerInitials } from "@/lib/wikipedia-upstream";
import type { HomeStory } from "@/services/home";

type StoryCardProps = {
  story: HomeStory;
  className?: string;
};

export function StoryCard({ story, className }: StoryCardProps) {
  return (
    <Link
      href={story.href}
      className={
        className ??
        "group flex w-[240px] shrink-0 flex-col text-foreground transition-opacity hover:opacity-90"
      }
    >
      <div className="relative aspect-[16/9] w-full overflow-hidden bg-surface-muted">
        {story.imageSrc ? (
          <Image
            src={story.imageSrc}
            alt={story.imageAlt}
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 240px"
            className="object-cover object-top transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-chrome">
            <span className="font-display text-[28px] font-bold tracking-wide text-white">
              {playerInitials(story.imageAlt)}
            </span>
          </div>
        )}
      </div>
      {story.imageCredit ? (
        <span className="mt-1 font-sans text-[11px] text-muted-foreground">{story.imageCredit}</span>
      ) : null}
      <span className="mt-3 font-sans text-[12px] font-medium text-muted-foreground">
        {story.tag}
      </span>
      <span className="mt-1 line-clamp-3 font-sans text-[18px] font-medium leading-snug text-foreground md:text-[20px]">
        {story.title}
      </span>
      {story.summary ? (
        <span className="mt-1 line-clamp-3 font-sans text-[14px] leading-snug text-muted-foreground">
          {story.summary}
        </span>
      ) : null}
      <span className="mt-2 font-sans text-[14px] text-muted-foreground">
        {story.publishedLabel}
        {story.readMinutes != null ? (
          <>
            <span className="mx-1.5 text-[12px]" aria-hidden>
              •
            </span>
            {story.readMinutes} min read
          </>
        ) : null}
      </span>
    </Link>
  );
}
