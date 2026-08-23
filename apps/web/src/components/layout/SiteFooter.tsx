import Link from "next/link";
import {
  FacebookIcon,
  InstagramIcon,
  XIcon,
  YoutubeIcon,
} from "@/components/ui/brandIcons";
import { footerColumns, socialLinks, type SocialLink } from "@/config/footer";

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
      className="inline-flex h-10 w-10 items-center justify-center text-white/80 transition-colors hover:text-uefa-gold"
    >
      <Icon className="h-5 w-5" />
    </a>
  );
}

export function SiteFooter() {
  const year = new Date().getFullYear();
  return (
    <footer className="mt-auto bg-chrome text-chrome-foreground">
      <div className="h-[3px] bg-wimbledon-green" />
      <div className="mx-auto max-w-[1400px] px-4 py-12 sm:px-6">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          {footerColumns.map((column) => (
            <nav key={column.id} aria-label={column.heading}>
              <p className="mb-3 font-data text-[11px] font-bold uppercase tracking-[0.16em] text-uefa-gold">
                {column.heading}
              </p>
              <ul className="space-y-2">
                {column.links.map((link) => (
                  <li key={link.id}>
                    <Link
                      href={link.href}
                      className="font-sans text-[14px] text-white/85 transition-colors hover:text-white"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
      </div>
      <div className="border-t border-white/10 bg-black">
        <div className="mx-auto flex max-w-[1400px] flex-col gap-4 px-4 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p className="font-sans text-[13px] text-white/60">
            © {year} Tennisly. Championship tennis visualisation.
          </p>
          <div className="flex items-center gap-1">
            {socialLinks.map((link) => (
              <SocialButton key={link.id} link={link} />
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
