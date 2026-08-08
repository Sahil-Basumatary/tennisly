package dev.sahilbasumatary.apigateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateLimitDecisionTest {

    private static final Instant WINDOW_START = Instant.parse("2026-08-08T12:34:10Z");

    @Test
    void fromCountAllowsAtLimit() {
        RateLimitDecision decision = RateLimitDecision.fromCount(30, 30, WINDOW_START);
        assertTrue(decision.allowed());
        assertEquals(0, decision.remaining());
        assertEquals(50, decision.retryAfterSeconds());
    }

    @Test
    void fromCountRejectsAboveLimit() {
        RateLimitDecision decision = RateLimitDecision.fromCount(31, 30, WINDOW_START);
        assertFalse(decision.allowed());
        assertEquals(0, decision.remaining());
        assertEquals(50, decision.retryAfterSeconds());
    }

    @Test
    void fromCountReportsRemainingBelowLimit() {
        RateLimitDecision decision = RateLimitDecision.fromCount(5, 30, WINDOW_START);
        assertTrue(decision.allowed());
        assertEquals(25, decision.remaining());
    }

    @Test
    void redisKeyUsesMinuteBucket() {
        assertEquals(
                "apikey-rl:org-123:202608081234",
                RateLimitDecision.redisKey("org-123", WINDOW_START));
    }

    @Test
    void secondsUntilNextMinuteAtLeastOne() {
        Instant lastSecond = Instant.parse("2026-08-08T12:34:59Z");
        assertEquals(1, RateLimitDecision.secondsUntilNextMinute(lastSecond));
    }
}
