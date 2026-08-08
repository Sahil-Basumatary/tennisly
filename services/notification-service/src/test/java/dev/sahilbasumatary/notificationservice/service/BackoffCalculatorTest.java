package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BackoffCalculatorTest {

    @Test
    void firstRetryDelayIs30Seconds() {
        Duration delay = BackoffCalculator.delayForAttempt(0);
        assertEquals(Duration.ofSeconds(30), delay);
    }

    @Test
    void secondRetryDelayIs2Minutes() {
        Duration delay = BackoffCalculator.delayForAttempt(1);
        assertEquals(Duration.ofMinutes(2), delay);
    }

    @Test
    void thirdRetryDelayIs10Minutes() {
        Duration delay = BackoffCalculator.delayForAttempt(2);
        assertEquals(Duration.ofMinutes(10), delay);
    }

    @Test
    void fourthRetryDelayIs1Hour() {
        Duration delay = BackoffCalculator.delayForAttempt(3);
        assertEquals(Duration.ofHours(1), delay);
    }

    @Test
    void fifthRetryDelayIs6Hours() {
        Duration delay = BackoffCalculator.delayForAttempt(4);
        assertEquals(Duration.ofHours(6), delay);
    }

    @Test
    void attemptsAboveMaxCapAt6Hours() {
        Duration delay = BackoffCalculator.delayForAttempt(99);
        assertEquals(Duration.ofHours(6), delay);
    }

    @Test
    void nextAttemptAtReturnsInstantInTheFuture() {
        Instant before = Instant.now();
        Instant next = BackoffCalculator.nextAttemptAt(0);
        assertTrue(next.isAfter(before));
        assertTrue(next.isBefore(before.plus(Duration.ofSeconds(35))));
    }
}
