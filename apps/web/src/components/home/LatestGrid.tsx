import { StoryCard } from "@/components/home/StoryCard";
import type { HomeStory } from "@/services/home";

type LatestGridProps = {
  title: string;
  stories: HomeStory[];
};

export function LatestGrid({ title, stories }: LatestGridProps) {
  return (
    <section className="bg-white py-12 md:py-16">
      <div className="mx-auto max-w-[1400px] px-6 md:px-10">
        <h2 className="mb-8 font-display text-[32px] font-bold leading-none tracking-tight text-foreground md:mb-10 md:text-[40px]">
          {title}
        </h2>
        <div className="grid grid-cols-1 gap-x-8 gap-y-10 sm:grid-cols-2 lg:grid-cols-4">
          {stories.map((story) => (
            <StoryCard
              key={story.id}
              story={story}
              className="group flex w-full flex-col text-foreground transition-opacity hover:opacity-90"
            />
          ))}
        </div>
      </div>
    </section>
  );
}
