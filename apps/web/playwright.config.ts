import { existsSync } from "node:fs";
import { defineConfig, devices } from "@playwright/test";

const port = process.env.PLAYWRIGHT_PORT ?? "3110";
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://localhost:${port}`;
const managedServer = !process.env.PLAYWRIGHT_BASE_URL;

if (managedServer && !existsSync(".next/BUILD_ID")) {
  throw new Error(
    "Missing production build (.next/BUILD_ID). Run `pnpm --filter @tennisly/web build` or use `pnpm test:e2e` (builds first). Do not use next dev/Turbopack for e2e.",
  );
}

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 20_000 },
  reporter: [["list"], ["html", { open: "never" }]],
  globalSetup: require.resolve("./e2e/global.setup.ts"),
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: managedServer
    ? {
        // next start only — next/turbopack dev is flaky with Clerk + Playwright
        command: `pnpm exec next start --port ${port} --hostname localhost`,
        url: `${baseURL}/api/health`,
        reuseExistingServer: false,
        timeout: 120_000,
      }
    : undefined,
});
