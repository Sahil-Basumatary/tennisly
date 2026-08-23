import { LegalPage } from "@/components/layout/LegalPage";

export default function CareersPage() {
  return (
    <LegalPage
      eyebrow="Club"
      title="Careers"
      summary="No open roles right now. The stack is Next.js, Spring Boot, Postgres, and a point tape."
    >
      <p>
        Tennisly is a solo-built championship product. When roles open they will be listed here
        with the same honesty as the analytics boards — no ghost jobs.
      </p>
      <p>
        Until then, send a short note to hello@tennisly.dev if you want to talk about live
        delivery, court visualisation, or tape-provable metrics.
      </p>
    </LegalPage>
  );
}
