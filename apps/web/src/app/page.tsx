import { EditorialCarousel } from "@/components/home/EditorialCarousel";
import { FeaturedBand } from "@/components/home/FeaturedBand";
import { HomeHero } from "@/components/home/HomeHero";
import { HomeScoreboard } from "@/components/home/HomeScoreboard";
import { LatestGrid } from "@/components/home/LatestGrid";
import { TourPulse } from "@/components/home/TourPulse";
import { getHomeContent } from "@/services/home";
import { getScoreboardDay } from "@/services/scaffolds";

export default async function HomePage() {
  const [content, day] = await Promise.all([getHomeContent(), getScoreboardDay()]);
  return (
    <main id="main-content">
      <HomeHero {...content.hero} />
      <HomeScoreboard day={day} />
      {content.tourPulse ? <TourPulse pulse={content.tourPulse} /> : null}
      {content.onCourt.length > 0 || content.onCourtMore.length > 0 ? (
        <EditorialCarousel title="On court" stories={content.onCourt} more={content.onCourtMore} />
      ) : null}
      {content.featured ? <FeaturedBand {...content.featured} /> : null}
      {content.playerProfiles.length > 0 ? (
        <LatestGrid
          title="Player profiles"
          stories={content.playerProfiles}
          moreHref="/players?view=rankings"
          moreLabel="Full rankings →"
        />
      ) : null}
    </main>
  );
}
