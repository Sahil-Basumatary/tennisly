import { LegalPage } from "@/components/layout/LegalPage";

export default function ContactPage() {
  return (
    <LegalPage
      eyebrow="Club"
      title="Contact"
      summary="Press, product, and scoring incidents all land in one inbox. Signed-in users manage email categories under Settings."
    >
      <p>
        Email{" "}
        <a className="font-semibold text-chrome underline" href="mailto:hello@tennisly.dev">
          hello@tennisly.dev
        </a>{" "}
        for press, bugs, or partnership notes.
      </p>
      <p>
        Live scoring incidents should include the match id from the URL and the time on the
        scorebug. That is enough for us to pull the tape.
      </p>
    </LegalPage>
  );
}
