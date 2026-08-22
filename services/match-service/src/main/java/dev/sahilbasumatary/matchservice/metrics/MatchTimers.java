package dev.sahilbasumatary.matchservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MatchTimers {

    private final AtomicInteger activeSessions = new AtomicInteger();
    private final AtomicInteger activeSubscriptions = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicInteger> subscriptionsBySession =
            new ConcurrentHashMap<>();
    private final Timer getMatch;
    private final Timer recordPoint;
    private final Timer livePublishAfterCommit;
    private final Counter liveCacheHit;
    private final Counter liveCacheMiss;
    private final Counter livePublishFailure;
    private final Counter liveSessionConnect;
    private final Counter liveSessionDisconnect;
    private final Counter liveBackpressureDisconnect;
    private final Counter liveSubscribeSuccess;
    private final Counter liveOutboundRejected;

    public MatchTimers(MeterRegistry registry) {
        this.getMatch =
                Timer.builder("match.get")
                        .publishPercentileHistogram()
                        .register(registry);
        this.recordPoint =
                Timer.builder("match.record_point")
                        .publishPercentileHistogram()
                        .register(registry);
        this.livePublishAfterCommit =
                Timer.builder("match.live_publish_after_commit")
                        .description("Commit callback to WebSocket broker publication")
                        .publishPercentileHistogram()
                        .register(registry);
        this.liveCacheHit = Counter.builder("match.live_cache").tag("result", "hit").register(registry);
        this.liveCacheMiss = Counter.builder("match.live_cache").tag("result", "miss").register(registry);
        this.livePublishFailure =
                Counter.builder("match.live_publish")
                        .tag("result", "failure")
                        .register(registry);
        this.liveSessionConnect =
                Counter.builder("match.live_session")
                        .tag("result", "connect")
                        .register(registry);
        this.liveSessionDisconnect =
                Counter.builder("match.live_session")
                        .tag("result", "disconnect")
                        .register(registry);
        this.liveBackpressureDisconnect =
                Counter.builder("match.live_session")
                        .tag("result", "backpressure")
                        .register(registry);
        this.liveSubscribeSuccess =
                Counter.builder("match.live_subscribe")
                        .tag("result", "success")
                        .register(registry);
        this.liveOutboundRejected =
                Counter.builder("match.live_outbound")
                        .tag("result", "rejected")
                        .register(registry);
        Gauge.builder("match.live_session.active", activeSessions, AtomicInteger::get)
                .description("Open WebSocket sessions on this instance")
                .register(registry);
        Gauge.builder("match.live_subscribe.active", activeSubscriptions, AtomicInteger::get)
                .description("Active STOMP subscriptions on this instance")
                .register(registry);
    }

    public void bindOutboundExecutor(ThreadPoolTaskExecutor executor, MeterRegistry registry) {
        Gauge.builder("match.live_outbound.queued", executor, value -> value.getThreadPoolExecutor().getQueue().size())
                .description("Queued client outbound frames")
                .register(registry);
        Gauge.builder("match.live_outbound.active", executor, value -> value.getThreadPoolExecutor().getActiveCount())
                .description("Active client outbound send threads")
                .register(registry);
    }

    public void sessionOpened() {
        activeSessions.incrementAndGet();
        liveSessionConnect.increment();
    }

    public void sessionClosed() {
        activeSessions.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void subscriptionOpened(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        subscriptionsBySession.computeIfAbsent(sessionId, key -> new AtomicInteger()).incrementAndGet();
        activeSubscriptions.incrementAndGet();
        liveSubscribeSuccess.increment();
    }

    public void subscriptionClosed(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        AtomicInteger count = subscriptionsBySession.get(sessionId);
        if (count == null || count.get() <= 0) {
            return;
        }
        count.decrementAndGet();
        activeSubscriptions.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void clearSessionSubscriptions(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        AtomicInteger count = subscriptionsBySession.remove(sessionId);
        if (count == null) {
            return;
        }
        int removed = count.getAndSet(0);
        if (removed > 0) {
            activeSubscriptions.updateAndGet(value -> Math.max(0, value - removed));
        }
    }

    public int activeSessionCount() {
        return activeSessions.get();
    }

    public int activeSubscriptionCount() {
        return activeSubscriptions.get();
    }

    public Timer getMatch() {
        return getMatch;
    }

    public Timer recordPoint() {
        return recordPoint;
    }

    public Timer livePublishAfterCommit() {
        return livePublishAfterCommit;
    }

    public Counter liveCacheHit() {
        return liveCacheHit;
    }

    public Counter liveCacheMiss() {
        return liveCacheMiss;
    }

    public Counter livePublishFailure() {
        return livePublishFailure;
    }

    public Counter liveSessionConnect() {
        return liveSessionConnect;
    }

    public Counter liveSessionDisconnect() {
        return liveSessionDisconnect;
    }

    public Counter liveBackpressureDisconnect() {
        return liveBackpressureDisconnect;
    }

    public Counter liveSubscribeSuccess() {
        return liveSubscribeSuccess;
    }

    public Counter liveOutboundRejected() {
        return liveOutboundRejected;
    }
}
