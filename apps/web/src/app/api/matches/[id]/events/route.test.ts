import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchUpstreamMatchEvents } from "@/lib/match-upstream";
import { resetSingleFlight } from "@/lib/single-flight";
import { GET } from "./route";

const EVENT = { id: "event-2", sequence: 2, eventType: "POINT_RECORDED" };

vi.mock("@/lib/match-upstream", () => ({
  fetchUpstreamMatchEvents: vi
    .fn()
    .mockResolvedValue([{ id: "event-2", sequence: 2, eventType: "POINT_RECORDED" }]),
  MatchUpstreamError: class MatchUpstreamError extends Error {},
}));

const MATCH_ID = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

function eventsRequest() {
  return GET(
    new Request(`http://localhost/api/matches/${MATCH_ID}/events?afterSequence=1`),
    { params: Promise.resolve({ id: MATCH_ID }) },
  );
}

describe("GET /api/matches/[id]/events", () => {
  beforeEach(() => {
    resetSingleFlight();
    vi.mocked(fetchUpstreamMatchEvents).mockReset();
    vi.mocked(fetchUpstreamMatchEvents).mockResolvedValue([EVENT]);
  });

  it("never shares recovery pages at the CDN", async () => {
    const response = await eventsRequest();
    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toContain("private");
    expect(response.headers.get("Cache-Control")).toContain("no-store");
    expect(response.headers.get("ETag")).toBeNull();
  });

  it("coalesces concurrent identical recovery pages onto one origin fetch", async () => {
    let release: () => void = () => {};
    const blocked = new Promise<void>((resolve) => {
      release = resolve;
    });
    let started = 0;
    vi.mocked(fetchUpstreamMatchEvents).mockImplementation(async () => {
      started += 1;
      await blocked;
      return [EVENT];
    });
    const pending = Promise.all([eventsRequest(), eventsRequest(), eventsRequest()]);
    await vi.waitFor(() => expect(started).toBe(1));
    release();
    const responses = await pending;
    expect(responses.every((row) => row.status === 200)).toBe(true);
    expect(fetchUpstreamMatchEvents).toHaveBeenCalledTimes(1);
  });
});
