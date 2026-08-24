import Link from "next/link";
import { RemotePhoto } from "@/components/media/RemotePhoto";
import { PORTRAIT_FIT } from "@/lib/headshot";
import { playerInitials } from "@/lib/wikipedia-upstream";
import type { EditorialStory } from "@/types/editorial";

export function StoryArticle({ story }: { story: EditorialStory }) {
  return (
    <article className="bg-background">
      <div className="bg-chrome text-chrome-foreground">
        <div className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6 sm:py-10">
          <nav aria-label="Breadcrumb" className="mb-4 font-sans text-[13px] text-white/70">
            <ol className="flex flex-wrap items-center gap-2">
              <li>
                <Link href="/" className="hover:text-white">
                  Home
                </Link>
              </li>
              <li aria-hidden>/</li>
              <li>
                <span>{story.label}</span>
              </li>
            </ol>
          </nav>
          <p className="mb-2 font-data text-[11px] font-bold uppercase tracking-[0.18em] text-white/70">
            {story.label}
          </p>
          <h1 className="max-w-[22ch] font-display text-[28px] font-bold uppercase leading-[1.05] tracking-tight sm:text-[36px]">
            {story.headline}
          </h1>
          <p className="mt-3 max-w-3xl font-sans text-[15px] leading-relaxed text-white/80">
            {story.dek}
          </p>
        </div>
        <div className="h-[3px] bg-court-green" />
      </div>
      <div className="mx-auto grid max-w-[1400px] gap-10 px-4 py-8 sm:px-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(16rem,0.7fr)]">
        <div>
          <div className="relative mx-auto aspect-[3/4] w-full max-w-[420px] overflow-hidden bg-surface-muted lg:mx-0">
            <RemotePhoto
              src={story.imageSrc}
              alt={story.imageAlt}
              className={PORTRAIT_FIT}
              fallback={
                <div className="absolute inset-0 flex items-center justify-center bg-chrome">
                  <span className="font-display text-[48px] font-bold tracking-wide text-white">
                    {playerInitials(story.imageAlt || story.headline)}
                  </span>
                </div>
              }
            />
          </div>
          {story.imageCredit ? (
            <p className="mt-2 font-sans text-[12px] text-muted-foreground">{story.imageCredit}</p>
          ) : null}
          {story.extract ? (
            <div className="mt-8">
              <p className="font-sans text-[17px] leading-relaxed text-foreground">{story.extract}</p>
              {story.sourceUrl ? (
                <p className="mt-3 font-sans text-[13px] text-muted-foreground">
                  Source:{" "}
                  <a
                    href={story.sourceUrl}
                    rel="noopener noreferrer"
                    target="_blank"
                    className="font-semibold text-chrome underline"
                  >
                    {story.sourceTitle ?? "Wikipedia"}
                  </a>
                </p>
              ) : (
                <p className="mt-3 font-sans text-[13px] text-muted-foreground">
                  Source extract is shown only when Wikipedia confirms a tennis player match.
                </p>
              )}
            </div>
          ) : (
            <p className="mt-8 font-sans text-[15px] text-muted-foreground">
              No validated Wikipedia extract for this {story.kind === "match" ? "match" : "player"} yet.
              Live facts below still come from Tennisly services.
            </p>
          )}
        </div>
        <aside>
          <h2 className="mb-3 font-data text-[12px] font-bold uppercase tracking-[0.14em] text-chrome">
            Current facts
          </h2>
          <dl className="border border-hairline bg-white">
            {story.facts.map((fact) => (
              <div key={fact.label} className="grid grid-cols-[7.5rem_1fr] border-b border-hairline last:border-b-0">
                <dt className="px-3 py-2.5 font-data text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
                  {fact.label}
                </dt>
                <dd className="px-3 py-2.5 font-sans text-[14px] font-medium">{fact.value}</dd>
              </div>
            ))}
          </dl>
          <div className="mt-6 flex flex-col gap-2">
            <Link
              href={story.primaryCta.href}
              className="inline-flex h-11 items-center justify-center bg-chrome px-4 font-sans text-[12px] font-bold uppercase tracking-wide text-chrome-foreground"
            >
              {story.primaryCta.label}
            </Link>
            {story.secondaryCta ? (
              <Link
                href={story.secondaryCta.href}
                className="inline-flex h-11 items-center justify-center border border-chrome px-4 font-sans text-[12px] font-bold uppercase tracking-wide text-chrome"
              >
                {story.secondaryCta.label}
              </Link>
            ) : null}
          </div>
          <h2 className="mb-3 mt-8 font-data text-[12px] font-bold uppercase tracking-[0.14em] text-chrome">
            Related
          </h2>
          <ul className="divide-y divide-hairline border border-hairline bg-white">
            {story.related.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  className="block px-3 py-2.5 font-sans text-[14px] hover:bg-surface-muted"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </aside>
      </div>
    </article>
  );
}
