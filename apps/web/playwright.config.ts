import { existsSync } from "node:fs";
import { loadEnvConfig } from "@next/env";
import { defineConfig, devices } from "@playwright/test";

loadEnvConfig(process.cwd());

const port = process.env.PLAYWRIGHT_PORT ?? "3110";
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://localhost:${port}`;
const managedServer = !process.env.PLAYWRIGHT_BASE_URL;
const hasAuthUser = Boolean(process.env.E2E_CLERK_USER_EMAIL);
const authState = "playwright/.clerk/user.json";

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
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "setup",
      testMatch: /global\.setup\.ts/,
    },
    {
      name: "chromium",
      testMatch: /(smoke|live-replay)\.spec\.ts/,
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["setup"],
    },
    ...(hasAuthUser
      ? [
          {
            name: "authenticated",
            testMatch: /authenticated\.spec\.ts/,
            use: {
              ...devices["Desktop Chrome"],
              storageState: authState,
            },
            dependencies: ["setup"],
          },
        ]
      : []),
  ],
  webServer: managedServer
    ? {
        command: `pnpm exec next start --port ${port} --hostname localhost`,
        url: `${baseURL}/api/health`,
        reuseExistingServer: false,
        timeout: 120_000,
      }
    : undefined,
});
