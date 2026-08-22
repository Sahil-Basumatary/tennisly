package dev.sahilbasumatary.matchservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MatchTimers {

    private final Timer getMatch;
    private final Timer recordPoint;
    private final Timer livePublishAfterCommit;
    private final Counter liveCacheHit;
    private final Counter liveCacheMiss;
    private final Counter livePublishFailure;

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
}
