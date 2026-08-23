import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { StoryArticle } from "@/components/editorial/StoryArticle";
import { PageHero } from "@/components/layout/PageHero";
import { getStory } from "@/services/stories";

type PageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const result = await getStory(slug);
  if (result.status !== "ok") {
    return { title: "Story" };
  }
  return {
    title: `${result.story.headline} · Tennisly`,
    description: result.story.dek,
  };
}

export default async function StoryPage({ params }: PageProps) {
  const { slug } = await params;
  const result = await getStory(slug);
  if (result.status === "missing") notFound();
  if (result.status === "unavailable") {
    return (
      <>
        <PageHero
          eyebrow="Stories"
          title="Story unavailable"
          description="This story is temporarily unavailable."
        />
        <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
          <p className="font-sans text-sm text-muted-foreground">
            This story is temporarily unavailable. Try again shortly.
          </p>
          <p className="mt-4 font-sans text-[13px]">
            <Link href="/" className="font-semibold text-chrome underline">
              Back to home
            </Link>
          </p>
        </main>
      </>
    );
  }
  return (
    <main id="main-content">
      <StoryArticle story={result.story} />
    </main>
  );
}
