package dev.sahilbasumatary.apigateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class GatewayTimers {

    private final Timer apiKeyHit;
    private final Timer apiKeyMiss;
    private final Timer rateLimitCheck;

    public GatewayTimers(MeterRegistry registry) {
        this.apiKeyHit =
                Timer.builder("gateway.api_key.validate")
                        .tag("cache", "hit")
                        .publishPercentileHistogram()
                        .register(registry);
        this.apiKeyMiss =
                Timer.builder("gateway.api_key.validate")
                        .tag("cache", "miss")
                        .publishPercentileHistogram()
                        .register(registry);
        this.rateLimitCheck =
                Timer.builder("gateway.rate_limit.check")
                        .publishPercentileHistogram()
                        .register(registry);
    }

    public Timer apiKeyHit() {
        return apiKeyHit;
    }

    public Timer apiKeyMiss() {
        return apiKeyMiss;
    }

    public Timer rateLimitCheck() {
        return rateLimitCheck;
    }
}
