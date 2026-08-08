package dev.sahilbasumatary.apigateway.ratelimit;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public record RateLimitDecision(
        boolean allowed, int limit, long count, int remaining, int retryAfterSeconds) {

    private static final DateTimeFormatter MINUTE_BUCKET =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    public static String minuteBucket(Instant instant) {
        return MINUTE_BUCKET.format(instant);
    }

    public static String redisKey(String orgId, Instant instant) {
        return "apikey-rl:" + orgId + ":" + minuteBucket(instant);
    }

    public static int secondsUntilNextMinute(Instant instant) {
        Instant nextMinute = instant.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES);
        long seconds = ChronoUnit.SECONDS.between(instant, nextMinute);
        return seconds <= 0 ? 1 : (int) seconds;
    }

    public static RateLimitDecision fromCount(long count, int limit, Instant instant) {
        int remaining = Math.max(0, limit - (int) count);
        int retryAfter = secondsUntilNextMinute(instant);
        boolean allowed = count <= limit;
        if (!allowed) {
            remaining = 0;
        }
        return new RateLimitDecision(allowed, limit, count, remaining, retryAfter);
    }
}
