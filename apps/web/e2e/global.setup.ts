import { mkdirSync } from "node:fs";
import path from "node:path";
import { clerk, clerkSetup } from "@clerk/testing/playwright";
import { loadEnvConfig } from "@next/env";
import { expect, test as setup } from "@playwright/test";

loadEnvConfig(process.cwd());
if (!process.env.CLERK_PUBLISHABLE_KEY && process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY) {
  process.env.CLERK_PUBLISHABLE_KEY = process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY;
}

setup.describe.configure({ mode: "serial" });

const authDir = path.join(__dirname, "../playwright/.clerk");
const authFile = path.join(authDir, "user.json");
const testUserEmail = process.env.E2E_CLERK_USER_EMAIL;

setup("clerk testing token", async () => {
  await clerkSetup();
});

if (testUserEmail) {
  setup("authenticate and save storage state", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await clerk.signIn({ page, emailAddress: testUserEmail });
    await page.goto("/dashboard", { waitUntil: "domcontentloaded" });
    await expect(page).not.toHaveURL(/sign-in/);
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
    mkdirSync(authDir, { recursive: true });
    await page.context().storageState({ path: authFile });
  });
}
