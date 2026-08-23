import { LegalPage } from "@/components/layout/LegalPage";

export default function AboutPage() {
  return (
    <LegalPage
      eyebrow="Club"
      title="About Tennisly"
      summary="Championship tennis visualisation — scores, rankings, and point-level replay you can audit."
    >
      <p>
        Tennisly is built so a live score, a ranking table, and a replayed point all come from the
        same committed tape. The public site is the Wimbledon-style window; the live ticker is the
        ESPN board; competitions are the UEFA rail.
      </p>
      <p>
        Match-service owns live scoring and WebSocket delivery. Tennis-data-service owns rankings
        and tournament metadata. Analytics-service only publishes metrics that can be recomputed
        from indexed points.
      </p>
      <p>
        We do not invent rally stats. If a number is on an analytics board, it was derived from
        stored points.
      </p>
    </LegalPage>
  );
}
