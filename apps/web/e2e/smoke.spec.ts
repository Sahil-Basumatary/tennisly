import { setupClerkTestingToken } from "@clerk/testing/playwright";
import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await setupClerkTestingToken({ page });
});

test.describe("public smoke", () => {
  test("health endpoint is up with security headers", async ({ request }) => {
    const response = await request.get("/api/health");
    expect(response.ok()).toBeTruthy();
    expect(await response.json()).toEqual({ status: "ok" });
    const headers = response.headers();
    expect(headers["x-content-type-options"]).toBe("nosniff");
    expect(headers["x-frame-options"]).toBe("DENY");
    expect(headers["referrer-policy"]).toBe("strict-origin-when-cross-origin");
    expect(headers["content-security-policy"]).toContain("frame-ancestors 'none'");
    expect(headers["permissions-policy"]).toContain("camera=()");
  });

  test("home renders brand and hero", async ({ page }) => {
    const response = await page.goto("/", { waitUntil: "domcontentloaded" });
    expect(response, "home should respond").not.toBeNull();
    expect(response!.status(), "home should not 5xx").toBeLessThan(500);
    await expect(
      page.locator("header").getByRole("link", { name: "Tennisly" }),
    ).toBeVisible();
    await expect(page.locator("#main-content")).toBeVisible();
    await expect(page.locator("#main-content h1")).toBeVisible();
  });

  test("about page is publicly reachable", async ({ page }) => {
    const response = await page.goto("/about", { waitUntil: "domcontentloaded" });
    expect(response, "about should respond").not.toBeNull();
    expect(response!.status(), "about should not 5xx").toBeLessThan(500);
    await expect(
      page.locator("header").getByRole("link", { name: "Tennisly" }),
    ).toBeVisible();
    await expect(page).not.toHaveURL(/sign-in/);
  });
});

test.describe("auth gates", () => {
  test("dashboard redirects unauthenticated users to sign-in", async ({ page }) => {
    await page.goto("/dashboard", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/sign-in/);
  });

  test("settings redirects unauthenticated users to sign-in", async ({ page }) => {
    await page.goto("/settings/notifications", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/sign-in/);
  });

  test("admin redirects unauthenticated users to sign-in", async ({ page }) => {
    await page.goto("/admin", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/sign-in/);
  });
});
