import Link from "next/link";
import { RemotePhoto } from "@/components/media/RemotePhoto";
import { PORTRAIT_FIT } from "@/lib/headshot";
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
      {story.imageSrc ? (
        <div className="relative aspect-[3/4] w-full overflow-hidden bg-surface-muted">
          <RemotePhoto
            src={story.imageSrc}
            alt={story.imageAlt}
            className={`${PORTRAIT_FIT} transition-transform duration-500 group-hover:scale-[1.03]`}
            fallback={null}
          />
        </div>
      ) : null}
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
      <span className="mt-2 font-sans text-[14px] text-muted-foreground">{story.publishedLabel}</span>
    </Link>
  );
}
