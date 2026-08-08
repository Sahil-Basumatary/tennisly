package dev.sahilbasumatary.notificationservice.service;

import java.time.Duration;
import java.time.Instant;

public final class BackoffCalculator {

    private static final Duration[] DELAYS = {
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofHours(6)
    };

    private BackoffCalculator() {}

    public static Instant nextAttemptAt(int attemptCount) {
        int index = Math.min(attemptCount, DELAYS.length - 1);
        return Instant.now().plus(DELAYS[index]);
    }

    public static Duration delayForAttempt(int attemptCount) {
        int index = Math.min(attemptCount, DELAYS.length - 1);
        return DELAYS[index];
    }
}
