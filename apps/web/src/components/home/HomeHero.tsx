import Image from "next/image";
import Link from "next/link";
import { ArrowRightIcon } from "@/components/ui/brandIcons";

type HomeHeroProps = {
  headline: string;
  ctaLabel: string;
  ctaHref: string;
  imageSrc: string;
  imageAlt: string;
};

export function HomeHero({
  headline,
  ctaLabel,
  ctaHref,
  imageSrc,
  imageAlt,
}: HomeHeroProps) {
  return (
    <section className="relative isolate min-h-[402px] w-full overflow-hidden bg-inverse-deep md:min-h-[520px]">
      <Image
        src={imageSrc}
        alt={imageAlt}
        fill
        priority
        sizes="100vw"
        className="object-cover object-center"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-inverse-deep via-inverse-deep/55 to-inverse-deep/20" />
      <div className="relative z-10 mx-auto flex min-h-[402px] max-w-[1400px] flex-col justify-end px-6 pb-10 pt-24 md:min-h-[520px] md:px-10 md:pb-14">
        <h1 className="max-w-[18ch] font-display text-[28px] font-bold uppercase leading-[1.1] tracking-tight text-white md:text-[32px] lg:text-[40px]">
          {headline}
        </h1>
        <Link
          href={ctaHref}
          className="mt-4 inline-flex w-fit items-center gap-2 font-sans text-[14px] font-semibold text-white transition-opacity hover:opacity-80"
        >
          {ctaLabel}
          <ArrowRightIcon className="h-4 w-4" />
        </Link>
      </div>
    </section>
  );
}
