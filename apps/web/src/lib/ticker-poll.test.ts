import { describe, expect, it } from "vitest";
import {
  TICKER_HIDDEN_MS,
  TICKER_VISIBLE_MS,
  shouldReplaceTickerBody,
  tickerIntervalMs,
} from "@/lib/ticker-poll";

describe("ticker poll", () => {
  it("slows the strip when the tab is in the background", () => {
    expect(tickerIntervalMs(false)).toBe(TICKER_VISIBLE_MS);
    expect(tickerIntervalMs(true)).toBe(TICKER_HIDDEN_MS);
  });

  it("keeps the previous strip on 304", () => {
    expect(shouldReplaceTickerBody(200)).toBe(true);
    expect(shouldReplaceTickerBody(304)).toBe(false);
    expect(shouldReplaceTickerBody(502)).toBe(false);
  });
});
