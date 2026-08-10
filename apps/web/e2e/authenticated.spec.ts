import { setupClerkTestingToken } from "@clerk/testing/playwright";
import { expect, test } from "@playwright/test";

const hasAuthUser = Boolean(process.env.E2E_CLERK_USER_EMAIL);

test.beforeEach(async ({ page }) => {
  test.skip(!hasAuthUser, "Set E2E_CLERK_USER_EMAIL to run authenticated e2e");
  await setupClerkTestingToken({ page });
});

test.describe("authenticated member surfaces", () => {
  test("dashboard is reachable when signed in", async ({ page }) => {
    await page.goto("/dashboard", { waitUntil: "domcontentloaded" });
    await expect(page).not.toHaveURL(/sign-in/);
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Preferences" })).toBeVisible();
  });

  test("notification settings is reachable when signed in", async ({ page }) => {
    await page.goto("/settings/notifications", { waitUntil: "domcontentloaded" });
    await expect(page).not.toHaveURL(/sign-in/);
    await expect(page.locator("#main-content")).toBeVisible();
  });
});
