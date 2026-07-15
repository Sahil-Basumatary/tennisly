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
      <EditorialCarousel title="Editor’s picks" stories={content.editorsPicks} />
      <FeaturedBand {...content.featured} />
      <LatestGrid title="Latest" stories={content.latest} />
    </main>
  );
}
