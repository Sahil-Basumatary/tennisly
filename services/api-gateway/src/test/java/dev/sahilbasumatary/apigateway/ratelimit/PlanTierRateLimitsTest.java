package dev.sahilbasumatary.apigateway.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sahilbasumatary.apigateway.config.PlanTierRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanTierRateLimitsTest {

    private PlanTierRateLimitProperties limits;

    @BeforeEach
    void setUp() {
        limits = new PlanTierRateLimitProperties();
    }

    @Test
    void normalizeTierDefaultsBlankToFree() {
        assertEquals("FREE", PlanTierRateLimits.normalizeTier(null));
        assertEquals("FREE", PlanTierRateLimits.normalizeTier(""));
        assertEquals("FREE", PlanTierRateLimits.normalizeTier("   "));
    }

    @Test
    void normalizeTierUppercasesKnownValues() {
        assertEquals("PRO", PlanTierRateLimits.normalizeTier("pro"));
        assertEquals("ENTERPRISE", PlanTierRateLimits.normalizeTier(" Enterprise "));
    }

    @Test
    void requestsPerMinuteResolvesEachTier() {
        assertEquals(30, PlanTierRateLimits.requestsPerMinute("FREE", limits));
        assertEquals(120, PlanTierRateLimits.requestsPerMinute("BASIC", limits));
        assertEquals(600, PlanTierRateLimits.requestsPerMinute("PRO", limits));
        assertEquals(3000, PlanTierRateLimits.requestsPerMinute("ENTERPRISE", limits));
    }

    @Test
    void requestsPerMinuteTreatsUnknownTierAsFree() {
        assertEquals(30, PlanTierRateLimits.requestsPerMinute("STARTER", limits));
        assertEquals(30, PlanTierRateLimits.requestsPerMinute(null, limits));
    }
}
