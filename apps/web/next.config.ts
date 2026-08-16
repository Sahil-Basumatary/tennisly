import type { NextConfig } from "next";

const extraConnectSrc = (process.env.CSP_CONNECT_SRC_EXTRA ?? "")
  .split(/\s+/)
  .map((s) => s.trim())
  .filter(Boolean)
  .join(" ");

// Production Clerk serves clerk-js from the FAPI satellite, not *.clerk.com.
const clerkSatellite = "https://clerk.tennisly.tv https://accounts.tennisly.tv";

const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline' 'unsafe-eval' https://*.clerk.accounts.dev https://*.clerk.com https://*.protect.clerk.com https://challenges.cloudflare.com ${clerkSatellite}`,
  [
    "connect-src 'self'",
    "https://*.clerk.accounts.dev",
    "https://*.clerk.com",
    "https://*.protect.clerk.com",
    "wss://*.clerk.accounts.dev",
    "wss://clerk.tennisly.tv",
    clerkSatellite,
    "http://localhost:*",
    "ws://localhost:*",
    extraConnectSrc,
  ]
    .filter(Boolean)
    .join(" "),
  "img-src 'self' data: blob: https://images.unsplash.com https://*.clerk.com https://img.clerk.com https://clerk.tennisly.tv",
  "style-src 'self' 'unsafe-inline'",
  "font-src 'self' data:",
  `frame-src https://*.clerk.accounts.dev https://*.clerk.com https://challenges.cloudflare.com ${clerkSatellite}`,
  `worker-src 'self' blob: ${clerkSatellite}`,
  "base-uri 'self'",
  "form-action 'self'",
  "object-src 'none'",
  "frame-ancestors 'none'",
].join("; ");

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  {
    key: "Permissions-Policy",
    value: "camera=(), microphone=(), geolocation=()",
  },
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
];

const nextConfig: NextConfig = {
  experimental: {
    optimizePackageImports: ["@babylonjs/core", "@visx/shape", "@visx/group", "framer-motion"],
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
      },
    ],
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: securityHeaders,
      },
    ];
  },
};

export default nextConfig;
