import Image from "next/image";
import Link from "next/link";

type FeaturedBandProps = {
  eyebrow: string;
  headline: string;
  label: string;
  href: string;
  imageSrc: string;
  imageAlt: string;
};

export function FeaturedBand({
  eyebrow,
  headline,
  label,
  href,
  imageSrc,
  imageAlt,
}: FeaturedBandProps) {
  return (
    <section className="relative isolate min-h-[320px] overflow-hidden bg-inverse-deep md:min-h-[420px]">
      <Image
        src={imageSrc}
        alt={imageAlt}
        fill
        sizes="100vw"
        className="object-cover object-center opacity-35"
      />
      <div className="absolute inset-0 bg-inverse-deep/75" />
      <Link
        href={href}
        className="relative z-10 mx-auto flex min-h-[320px] max-w-[1400px] flex-col items-center justify-center px-6 py-16 text-center md:min-h-[420px] md:px-10"
      >
        <p className="font-display text-[13px] font-light uppercase tracking-[0.28em] text-white/70 md:text-[14px]">
          {eyebrow}
        </p>
        <h2 className="mt-3 max-w-[16ch] font-display text-[36px] font-bold uppercase leading-[1.05] tracking-tight text-white md:text-[48px]">
          {headline}
        </h2>
        <span className="absolute bottom-6 left-6 font-sans text-[12px] font-medium text-white/80 md:left-10">
          {label}
        </span>
      </Link>
    </section>
  );
}
