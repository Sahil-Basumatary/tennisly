import { describe, expect, it } from "vitest";
import {
  COMPLETED_LIVE_CACHE_CONTROL,
  LIVE_CACHE_CONTROL,
  PRIVATE_NO_STORE,
  TICKER_CACHE_CONTROL,
  jsonPublic,
  sequenceEtag,
  strongEtag,
} from "@/lib/public-http-cache";

describe("public http cache", () => {
  it("builds a stable strong etag from the body", () => {
    expect(strongEtag('{"a":1}')).toMatch(/^"[0-9a-f]{16}"$/);
    expect(strongEtag('{"a":1}')).toBe(strongEtag('{"a":1}'));
    expect(strongEtag('{"a":1}')).not.toBe(strongEtag('{"a":2}'));
  });

  it("derives live etags from sequence so completed matches stay immutable", () => {
    expect(sequenceEtag("live-abc", 12)).toBe('"live-abc-12"');
    expect(COMPLETED_LIVE_CACHE_CONTROL).toContain("immutable");
    expect(LIVE_CACHE_CONTROL).toContain("s-maxage=2");
    expect(TICKER_CACHE_CONTROL).toContain("s-maxage=3");
  });

  it("returns 304 when If-None-Match matches", async () => {
    const body = { id: "m1", liveSequence: 4 };
    const etag = sequenceEtag("live-m1", 4);
    const response = jsonPublic(
      new Request("http://localhost/api/matches/m1/live", {
        headers: { "If-None-Match": etag },
      }),
      body,
      LIVE_CACHE_CONTROL,
      { etag },
    );
    expect(response.status).toBe(304);
    expect(response.headers.get("ETag")).toBe(etag);
    expect(response.headers.get("Cache-Control")).toBe(LIVE_CACHE_CONTROL);
    expect(response.headers.get("Vary")).toBe("Accept-Encoding");
    expect(await response.text()).toBe("");
  });

  it("does not vary on cookies and refuses to share oversized payloads", () => {
    const huge = { items: "x".repeat(70_000) };
    const response = jsonPublic(
      new Request("http://localhost/api/matches/ticker"),
      huge,
      TICKER_CACHE_CONTROL,
      { maxBytes: 64_000 },
    );
    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe(PRIVATE_NO_STORE);
    expect(response.headers.get("Vary")).toBeNull();
  });
});
