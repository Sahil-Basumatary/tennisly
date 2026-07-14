import Image from "next/image";
import Link from "next/link";
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
        <Image
          src={story.imageSrc}
          alt={story.imageAlt}
          fill
          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 240px"
          className="object-cover transition-transform duration-500 group-hover:scale-[1.03]"
        />
      </div>
      <span className="mt-3 font-sans text-[12px] font-medium text-muted-foreground">
        {story.tag}
      </span>
      <span className="mt-1 line-clamp-3 font-sans text-[18px] font-medium leading-snug text-foreground md:text-[20px]">
        {story.title}
      </span>
      <span className="mt-2 font-sans text-[14px] text-muted-foreground">
        {story.publishedLabel}
        <span className="mx-1.5 text-[12px]" aria-hidden>
          •
        </span>
        {story.readMinutes} min read
      </span>
    </Link>
  );
}
