import { setupClerkTestingToken } from "@clerk/testing/playwright";
import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await setupClerkTestingToken({ page });
});

test.describe("homepage reconstructed replay", () => {
  test("renders the reconstructed court first and keeps an h1", async ({ page }) => {
    const response = await page.goto("/", { waitUntil: "domcontentloaded" });
    expect(response, "home should respond").not.toBeNull();
    expect(response!.status()).toBeLessThan(500);
    await expect(page.locator("#main-content")).toBeVisible();
    await expect(page.locator("#main-content h1")).toBeVisible();
    await expect(page.getByText("Reconstructed live visualization").first()).toBeVisible();
    await expect(page.locator("#main-content canvas[role='img']")).toBeVisible();
  });

  test("does not fetch Babylon on the first homepage load", async ({ page }) => {
    const babylon: string[] = [];
    page.on("request", (request) => {
      if (/babylon/i.test(request.url())) babylon.push(request.url());
    });
    await page.goto("/", { waitUntil: "networkidle" });
    expect(babylon, "homepage must not download Babylon").toEqual([]);
  });

  test("click-to-play stays idle until Play, then transport appears when a replay loads", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const play = page.getByRole("button", { name: "Play reconstructed rally" });
    if ((await play.count()) === 0) {
      await expect(page.locator("#main-content h1")).toBeVisible();
      return;
    }
    await expect(page.getByLabel("Scrub rally time")).toHaveCount(0);
    await play.click();
    await expect(play).toHaveCount(0);
    const transport = page.getByLabel("Scrub rally time");
    const missing = page.getByText("Replay is not available for this match yet.");
    await expect(transport.or(missing)).toBeVisible({ timeout: 20_000 });
  });

  test("stays usable on a phone viewport without WebGL", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.addInitScript(() => {
      const original = HTMLCanvasElement.prototype.getContext;
      HTMLCanvasElement.prototype.getContext = function (
        this: HTMLCanvasElement,
        type: string,
        ...rest: unknown[]
      ) {
        if (type === "webgl" || type === "webgl2" || type === "experimental-webgl") return null;
        return original.call(this, type, ...(rest as []));
      } as typeof HTMLCanvasElement.prototype.getContext;
    });
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#main-content h1")).toBeVisible();
    await expect(page.locator("#main-content canvas[role='img']")).toBeVisible();
  });

  test("respects reduced motion and still shows the court", async ({ page }) => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#main-content h1")).toBeVisible();
    await expect(page.getByText("Reconstructed live visualization").first()).toBeVisible();
  });
});
