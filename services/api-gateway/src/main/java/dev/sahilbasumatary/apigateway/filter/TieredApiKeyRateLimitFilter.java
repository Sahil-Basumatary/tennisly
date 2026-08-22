package dev.sahilbasumatary.apigateway.filter;

import dev.sahilbasumatary.apigateway.config.ApiKeyAuthProperties;
import dev.sahilbasumatary.apigateway.config.PlanTierRateLimitProperties;
import dev.sahilbasumatary.apigateway.metrics.GatewayTimers;
import dev.sahilbasumatary.apigateway.ratelimit.PlanTierRateLimits;
import dev.sahilbasumatary.apigateway.ratelimit.RateLimitDecision;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TieredApiKeyRateLimitFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TieredApiKeyRateLimitFilter.class);
    private static final String PUBLIC_API_PREFIX = "/api/v1/";
    private static final Duration KEY_TTL = Duration.ofSeconds(60);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final PlanTierRateLimitProperties rateLimits;
    private final ApiKeyAuthProperties authProperties;
    private final GatewayTimers timers;

    public TieredApiKeyRateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            PlanTierRateLimitProperties rateLimits,
            ApiKeyAuthProperties authProperties) {
        this(redisTemplate, rateLimits, authProperties, null);
    }

    @Autowired
    public TieredApiKeyRateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            PlanTierRateLimitProperties rateLimits,
            ApiKeyAuthProperties authProperties,
            GatewayTimers timers) {
        this.redisTemplate = redisTemplate;
        this.rateLimits = rateLimits;
        this.authProperties = authProperties;
        this.timers = timers;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(PUBLIC_API_PREFIX)) {
            return chain.filter(exchange);
        }
        String orgId = exchange.getRequest().getHeaders().getFirst(ApiKeyAuthHeaders.X_ORG_ID);
        if (orgId == null || orgId.isBlank()) {
            return chain.filter(exchange);
        }
        String planTier =
                PlanTierRateLimits.normalizeTier(
                        exchange.getRequest().getHeaders().getFirst(ApiKeyAuthHeaders.X_PLAN_TIER));
        int limit = PlanTierRateLimits.requestsPerMinute(planTier, rateLimits);
        Instant now = Instant.now();
        String redisKey = RateLimitDecision.redisKey(orgId, now);
        Timer.Sample sample = timers == null ? null : Timer.start();
        return redisTemplate
                .opsForValue()
                .increment(redisKey)
                .flatMap(
                        count ->
                                count == 1
                                        ? redisTemplate
                                                .expire(redisKey, KEY_TTL)
                                                .thenReturn(count)
                                        : Mono.just(count))
                .map(count -> RateLimitDecision.fromCount(count, limit, now))
                .doOnSuccess(d -> stopRateLimitTimer(sample))
                .flatMap(
                        decision -> {
                            exchange.getResponse()
                                    .getHeaders()
                                    .set("X-RateLimit-Limit", String.valueOf(decision.limit()));
                            exchange.getResponse()
                                    .getHeaders()
                                    .set(
                                            "X-RateLimit-Remaining",
                                            String.valueOf(decision.remaining()));
                            if (decision.allowed()) {
                                return chain.filter(exchange);
                            }
                            return rateLimitExceeded(exchange, planTier, decision);
                        })
                .onErrorResume(
                        ex -> {
                            log.error(
                                    "Redis rate limit check failed for org {}: {}",
                                    orgId,
                                    ex.getMessage());
                            if (authProperties.isRateLimitFailOpen()) {
                                return chain.filter(exchange);
                            }
                            return rateLimitUnavailable(exchange);
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    private void stopRateLimitTimer(Timer.Sample sample) {
        if (sample != null && timers != null) {
            sample.stop(timers.rateLimitCheck());
        }
    }

    private Mono<Void> rateLimitExceeded(
            ServerWebExchange exchange, String planTier, RateLimitDecision decision) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse()
                .getHeaders()
                .set("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        String body =
                "{\"error\":\"rate_limit_exceeded\",\"planTier\":\""
                        + planTier
                        + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> rateLimitUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", "5");
        byte[] bytes =
                "{\"error\":\"rate_limit_unavailable\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
