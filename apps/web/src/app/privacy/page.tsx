import { LegalPage } from "@/components/layout/LegalPage";

export default function PrivacyPage() {
  return (
    <LegalPage
      title="Privacy"
      summary="What Tennisly collects for accounts, live boards, and optional alerts."
    >
      <p>
        Account sign-in is handled by Clerk. We receive the identifiers Clerk needs to keep a
        session (user id, email, display name). We do not sell that data.
      </p>
      <p>
        Match scores, rankings, and point tapes are ingested from tennis data providers so we can
        render live boards and replays. Those records are sports data, not your personal profile,
        unless you appear as a named player in a public ranking feed.
      </p>
      <p>
        If you enable notification categories, we store those switches against your account and
        may hold a device token so we can deliver the alerts you asked for. You can turn them off
        under Settings.
      </p>
      <p>
        Logs used to operate the site (request ids, error traces) are kept only long enough to
        debug delivery. Email hello@tennisly.dev to ask what we hold on your account or to request
        deletion.
      </p>
    </LegalPage>
  );
}
