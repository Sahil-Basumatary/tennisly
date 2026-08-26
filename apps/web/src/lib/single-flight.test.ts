import { describe, expect, it } from "vitest";
import { resetSingleFlight, singleFlight } from "@/lib/single-flight";

describe("singleFlight", () => {
  it("coalesces concurrent callers onto one producer", async () => {
    resetSingleFlight();
    let runs = 0;
    const run = () =>
      singleFlight("ticker", async () => {
        runs += 1;
        await Promise.resolve();
        return runs;
      });
    const [a, b, c] = await Promise.all([run(), run(), run()]);
    expect(a).toBe(1);
    expect(b).toBe(1);
    expect(c).toBe(1);
    expect(runs).toBe(1);
  });
});
