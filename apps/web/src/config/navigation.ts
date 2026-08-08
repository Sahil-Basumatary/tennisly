export type NavItem = {
  id: string;
  label: string;
  href: string;
  children?: NavItem[];
};

export const primaryNav: NavItem[] = [
  {
    id: "scores",
    label: "Scores",
    href: "/scores",
    children: [
      { id: "scores-live", label: "Live", href: "/scores?status=live" },
      { id: "scores-upcoming", label: "Upcoming", href: "/scores?status=upcoming" },
      { id: "scores-results", label: "Results", href: "/scores?status=final" },
    ],
  },
  {
    id: "tournaments",
    label: "Tournaments",
    href: "/tournaments",
    children: [
      { id: "tournaments-atp", label: "ATP", href: "/tournaments?tour=atp" },
      { id: "tournaments-wta", label: "WTA", href: "/tournaments?tour=wta" },
      { id: "tournaments-grand-slam", label: "Grand Slams", href: "/tournaments?level=grand_slam" },
    ],
  },
  {
    id: "players",
    label: "Players",
    href: "/players",
    children: [
      { id: "players-rankings", label: "Rankings", href: "/players?view=rankings" },
      { id: "players-search", label: "Search", href: "/players?view=search" },
    ],
  },
  {
    id: "matches",
    label: "Matches",
    href: "/matches",
    children: [
      { id: "matches-live", label: "Live Centre", href: "/matches?status=live" },
      { id: "matches-replays", label: "Replays", href: "/matches?view=replays" },
    ],
  },
  {
    id: "analytics",
    label: "Analytics",
    href: "/analytics",
    children: [
      { id: "analytics-overview", label: "Overview", href: "/analytics" },
      { id: "analytics-players", label: "Player", href: "/analytics/players" },
      { id: "analytics-compare", label: "Compare", href: "/analytics/compare" },
      { id: "analytics-tournaments", label: "Tournaments", href: "/analytics/tournaments" },
    ],
  },
];

export const utilityNav: NavItem[] = [
  { id: "dashboard", label: "Dashboard", href: "/dashboard" },
  { id: "admin", label: "Admin", href: "/admin" },
];
