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

  test("home editorial cards open story pages when the feed has data", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const story = page.locator('a[href^="/stories/"]');
    if ((await story.count()) === 0) {
      await expect(page.locator("#main-content")).toBeVisible();
      return;
    }
    await story.first().click();
    await expect(page).toHaveURL(/\/stories\//);
    await expect(
      page.getByRole("link", { name: /Open Match Centre|Open Player Analytics/ }),
    ).toBeVisible();
  });

  test("unknown story slugs 404 instead of inventing copy", async ({ page }) => {
    const response = await page.goto("/stories/not-a-real-slug", {
      waitUntil: "domcontentloaded",
    });
    expect(response, "missing story should respond").not.toBeNull();
    expect(response!.status()).toBe(404);
  });

  test("rankings stay on shareable page URLs", async ({ page }) => {
    await page.goto("/players?view=rankings&page=1", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#main-content")).toBeVisible();
    const next = page.getByRole("link", { name: "Next" });
    if ((await next.count()) === 0) {
      await expect(page.locator("#main-content")).toContainText(/Showing |No ranked|No rankings|players/i);
      return;
    }
    await next.click();
    await expect(page).toHaveURL(/page=2/);
    await expect(page.getByRole("link", { name: "Previous" })).toBeVisible();
  });

  test("masthead stays charcoal and competitions remain in the global rail", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const header = page.locator("header");
    await expect(header).toBeVisible();
    const bg = await header.evaluate((el) => getComputedStyle(el).backgroundColor);
    expect(bg).toMatch(/rgb\(\s*(29|30|31|32)\s*,\s*(29|30|31|32)\s*,\s*(29|30|31|32)\s*\)/);
    await expect(page.getByRole("navigation", { name: "Competitions" })).toBeVisible();
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
