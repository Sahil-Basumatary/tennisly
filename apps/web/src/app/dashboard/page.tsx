import Link from "next/link";
import { PageHero } from "@/components/layout/PageHero";

export default function DashboardPage() {
  return (
    <>
      <PageHero
        eyebrow="Account"
        title="Dashboard"
        description="Jump back into live scores, replays, and tape-provable analytics."
      />
      <main id="main-content" className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Link href="/scores" className="min-h-[140px] border border-hairline bg-white p-5 hover:border-chrome">
            <h2 className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide">Scores</h2>
            <p className="font-sans text-[13px] text-muted-foreground">
              ESPN-style board for every tour on the ticker.
            </p>
          </Link>
          <Link href="/matches" className="min-h-[140px] border border-hairline bg-white p-5 hover:border-chrome">
            <h2 className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide">Live Centre</h2>
            <p className="font-sans text-[13px] text-muted-foreground">
              Open the court, scorebug, and point tape.
            </p>
          </Link>
          <Link
            href="/settings/notifications"
            className="min-h-[140px] border border-hairline bg-white p-5 hover:border-chrome"
          >
            <h2 className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide">Preferences</h2>
            <p className="font-sans text-[13px] text-muted-foreground">
              Email categories and master switches.
            </p>
          </Link>
        </div>
      </main>
    </>
  );
}
