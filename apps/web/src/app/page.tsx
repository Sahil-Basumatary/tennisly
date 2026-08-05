import { EditorialCarousel } from "@/components/home/EditorialCarousel";
import { FeaturedBand } from "@/components/home/FeaturedBand";
import { HomeHero } from "@/components/home/HomeHero";
import { LatestGrid } from "@/components/home/LatestGrid";
import { getHomeContent } from "@/services/home";

export default async function HomePage() {
  const content = await getHomeContent();

  return (
    <main id="main-content">
      <HomeHero {...content.hero} />
      {content.editorsPicks.length > 0 ? (
        <EditorialCarousel title="Editor’s picks" stories={content.editorsPicks} />
      ) : null}
      {content.featured ? <FeaturedBand {...content.featured} /> : null}
      {content.latest.length > 0 ? <LatestGrid title="Latest" stories={content.latest} /> : null}
    </main>
  );
}
