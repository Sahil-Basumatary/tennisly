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

export type FooterColumn = {
  id: string;
  heading: string;
  links: FooterLink[];
};

export const footerColumns: FooterColumn[] = [
  {
    id: "watch",
    heading: "Watch",
    links: [
      { id: "scores", label: "Scores", href: "/scores" },
      { id: "live", label: "Live Centre", href: "/matches" },
      { id: "replays", label: "Replays", href: "/matches?view=replays" },
    ],
  },
  {
    id: "compete",
    heading: "Competitions",
    links: [
      { id: "slams", label: "Grand Slams", href: "/tournaments?level=grand_slam" },
      { id: "atp", label: "ATP Tour", href: "/tournaments?tour=atp" },
      { id: "wta", label: "WTA Tour", href: "/tournaments?tour=wta" },
    ],
  },
  {
    id: "players",
    heading: "Players",
    links: [
      { id: "atp-rank", label: "ATP Rankings", href: "/players?tour=atp" },
      { id: "wta-rank", label: "WTA Rankings", href: "/players?tour=wta" },
      { id: "analytics", label: "Analytics", href: "/analytics" },
    ],
  },
  {
    id: "club",
    heading: "Tennisly",
    links: [
      { id: "about", label: "About", href: "/about" },
      { id: "careers", label: "Careers", href: "/careers" },
      { id: "contact", label: "Contact", href: "/contact" },
      { id: "privacy", label: "Privacy", href: "/privacy" },
      { id: "terms", label: "Terms of Use", href: "/terms" },
    ],
  },
];

export const footerLinks: FooterLink[] = footerColumns.flatMap((column) => column.links);

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
