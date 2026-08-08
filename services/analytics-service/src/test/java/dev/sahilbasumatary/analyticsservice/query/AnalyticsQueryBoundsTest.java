package dev.sahilbasumatary.analyticsservice.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class AnalyticsQueryBoundsTest {

    @Test
    void clampPageSizeUsesDefaultWhenMissing() {
        assertEquals(20, AnalyticsQueryBounds.clampPageSize(null));
        assertEquals(20, AnalyticsQueryBounds.clampPageSize(0));
    }

    @Test
    void clampPageSizeCapsAtMax() {
        assertEquals(100, AnalyticsQueryBounds.clampPageSize(500));
        assertEquals(50, AnalyticsQueryBounds.clampPageSize(50));
    }

    @Test
    void validateDateRangeRejectsInvertedRange() {
        Instant from = Instant.parse("2024-06-01T00:00:00Z");
        Instant to = Instant.parse("2024-05-01T00:00:00Z");
        assertThrows(BadRequestException.class, () -> AnalyticsQueryBounds.validateDateRange(from, to));
    }

    @Test
    void validateDateRangeRejectsSpanOverOneYear() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = from.plus(367, ChronoUnit.DAYS);
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> AnalyticsQueryBounds.validateDateRange(from, to));
        assertTrue(ex.getMessage().contains("366"));
    }

    @Test
    void validateDateRangeAllowsPartialBounds() {
        AnalyticsQueryBounds.validateDateRange(Instant.parse("2024-01-01T00:00:00Z"), null);
        AnalyticsQueryBounds.validateDateRange(null, Instant.parse("2024-12-31T00:00:00Z"));
    }

    @Test
    void requireUuidRejectsBlank() {
        assertThrows(BadRequestException.class, () -> AnalyticsQueryBounds.requireUuid("  ", "playerA"));
    }
}
