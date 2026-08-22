package dev.sahilbasumatary.matchservice.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MatchTimersTest {

    @Test
    void tracksActiveSessionsAndSubscriptionsPerSession() {
        MatchTimers timers = new MatchTimers(new SimpleMeterRegistry());

        timers.sessionOpened();
        timers.sessionOpened();
        timers.subscriptionOpened("sess-1");
        timers.subscriptionOpened("sess-1");
        timers.subscriptionOpened("sess-2");

        assertEquals(2, timers.activeSessionCount());
        assertEquals(3, timers.activeSubscriptionCount());
        assertEquals(2, timers.liveSessionConnect().count());
        assertEquals(3, timers.liveSubscribeSuccess().count());

        timers.subscriptionClosed("sess-1");
        timers.clearSessionSubscriptions("sess-2");
        timers.sessionClosed();
        timers.sessionClosed();
        timers.sessionClosed();

        assertEquals(0, timers.activeSessionCount());
        assertEquals(1, timers.activeSubscriptionCount());

        timers.clearSessionSubscriptions("sess-1");
        assertEquals(0, timers.activeSubscriptionCount());
    }
}
