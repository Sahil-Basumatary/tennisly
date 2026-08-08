package dev.sahilbasumatary.analyticsservice.query;

import dev.sahilbasumatary.analyticsservice.exception.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class AnalyticsQueryBounds {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_TREND_SIZE = 100;
    public static final int MAX_EXPORT_ROWS = 1000;
    public static final int MAX_COMPARE_MEETINGS = 100;
    public static final int MAX_TOP_PLAYERS = 20;
    public static final long MAX_DATE_RANGE_DAYS = 366;

    private AnalyticsQueryBounds() {}

    public static int clampPageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static int clampPage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public static int clampTrendSize(Integer size) {
        if (size == null || size < 1) {
            return MAX_TREND_SIZE;
        }
        return Math.min(size, MAX_TREND_SIZE);
    }

    public static void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        if (from == null || to == null) {
            return;
        }
        long days = Duration.between(from, to).toDays();
        if (days > MAX_DATE_RANGE_DAYS) {
            throw new BadRequestException(
                    "Date range must not exceed " + MAX_DATE_RANGE_DAYS + " days");
        }
    }

    public static UUID requireUuid(String raw, String paramName) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(paramName + " is required");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(paramName + " must be a valid UUID");
        }
    }

    public static String requireNonBlank(String raw, String paramName) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(paramName + " is required");
        }
        return raw.trim();
    }
}
