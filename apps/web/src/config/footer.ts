export type FooterLink = {
  id: string;
  label: string;
  href: string;
  external?: boolean;
};

export type SocialLink = {
  id: "facebook" | "x" | "instagram" | "youtube";
  label: string;
  href: string;
};

export const footerLinks: FooterLink[] = [
  { id: "about", label: "About", href: "/about" },
  { id: "coverage", label: "Coverage", href: "/scores" },
  { id: "privacy", label: "Privacy", href: "/privacy" },
  { id: "terms", label: "Terms of Use", href: "/terms" },
  { id: "careers", label: "Careers", href: "/careers" },
  { id: "contact", label: "Contact", href: "/contact" },
];

export const socialLinks: SocialLink[] = [
  {
    id: "facebook",
    label: "Tennisly on Facebook",
    href: "https://www.facebook.com/",
  },
  {
    id: "x",
    label: "Tennisly on X",
    href: "https://x.com/",
  },
  {
    id: "instagram",
    label: "Tennisly on Instagram",
    href: "https://www.instagram.com/",
  },
  {
    id: "youtube",
    label: "Tennisly on YouTube",
    href: "https://www.youtube.com/",
  },
];
