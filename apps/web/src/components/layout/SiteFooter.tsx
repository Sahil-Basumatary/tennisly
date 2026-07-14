import Link from "next/link";
import {
  FacebookIcon,
  InstagramIcon,
  XIcon,
  YoutubeIcon,
} from "@/components/ui/brandIcons";
import { footerLinks, socialLinks, type SocialLink } from "@/config/footer";

const socialIconMap = {
  facebook: FacebookIcon,
  x: XIcon,
  instagram: InstagramIcon,
  youtube: YoutubeIcon,
} as const;

function SocialButton({ link }: { link: SocialLink }) {
  const Icon = socialIconMap[link.id];
  return (
    <a
      href={link.href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={link.label}
      className="inline-flex h-11 w-11 items-center justify-center text-foreground transition-opacity hover:opacity-70"
    >
      <Icon className="h-5 w-5" />
    </a>
  );
}

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="mt-auto bg-white">
      <div className="mx-auto max-w-[1400px] px-6 md:px-10">
        <div className="flex flex-col gap-6 py-8 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="font-display text-[18px] font-bold tracking-tight text-foreground"
            >
              Tennisly
            </Link>
            <span className="hidden h-8 w-px bg-hairline sm:block" aria-hidden />
            <p className="font-sans text-[13px] text-muted-foreground">
              Championship tennis visualisation
            </p>
          </div>
          <div className="flex items-center gap-1">
            {socialLinks.map((link) => (
              <SocialButton key={link.id} link={link} />
            ))}
          </div>
        </div>
        <div className="h-px w-full bg-brand-gold/40" aria-hidden />
        <div className="flex flex-col gap-6 py-8">
          <nav
            aria-label="Footer"
            className="flex flex-wrap items-center gap-x-6 gap-y-3"
          >
            {footerLinks.map((link) => (
              <Link
                key={link.id}
                href={link.href}
                className="font-sans text-[16px] font-medium text-primary transition-opacity hover:opacity-70"
              >
                {link.label}
              </Link>
            ))}
          </nav>
          <p className="font-sans text-[14px] text-muted-foreground md:text-[16px]">
            © {year} Tennisly. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
