import { LegalPage } from "@/components/layout/LegalPage";

export default function TermsPage() {
  return (
    <LegalPage
      title="Terms of Use"
      summary="By using Tennisly you agree to use the boards, replays, and account tools as published. These terms describe the product as it ships."
    >
      <p>
        Tennisly provides championship tennis scores, rankings, analytics derived from indexed
        point tapes, and court replay. Live numbers come from upstream match and ranking services.
        If a feed is down, boards stay empty rather than inventing a score.
      </p>
      <p>
        You must be allowed to create a Clerk account to use signed-in features (settings, saved
        analytics views, admin). Do not attempt to access another organisation's keys, webhooks, or
        audit logs.
      </p>
      <p>
        Content on the site is for information and visualisation. It is not betting advice. Replay
        geometry is reconstructed from stored points; overlays marked as synthesised are labelled
        in the court UI.
      </p>
      <p>
        We may suspend accounts that abuse the APIs or impersonate operators. Questions:
        hello@tennisly.dev.
      </p>
    </LegalPage>
  );
}
