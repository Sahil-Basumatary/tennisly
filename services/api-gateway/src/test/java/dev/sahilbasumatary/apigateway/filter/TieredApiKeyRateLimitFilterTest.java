package dev.sahilbasumatary.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.apigateway.config.ApiKeyAuthProperties;
import dev.sahilbasumatary.apigateway.config.PlanTierRateLimitProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class TieredApiKeyRateLimitFilterTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;
    private ApiKeyAuthProperties authProperties;
    private TieredApiKeyRateLimitFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        authProperties = new ApiKeyAuthProperties();
        filter =
                new TieredApiKeyRateLimitFilter(
                        redisTemplate, new PlanTierRateLimitProperties(), authProperties);
    }

    @Test
    void skipsNonPublicApiPaths() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/matches").build());
        WebFilterChain chain = e -> Mono.empty();
        filter.filter(exchange, chain).block();
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void failOpenAllowsRequestWhenRedisErrors() {
        authProperties.setRateLimitFailOpen(true);
        when(valueOps.increment(anyString()))
                .thenReturn(Mono.error(new RuntimeException("redis down")));
        MockServerWebExchange exchange = publicApiExchange();
        WebFilterChain chain = e -> Mono.empty();
        filter.filter(exchange, chain).block();
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void failClosedReturns503WhenRedisErrors() {
        authProperties.setRateLimitFailOpen(false);
        when(valueOps.increment(anyString()))
                .thenReturn(Mono.error(new RuntimeException("redis down")));
        MockServerWebExchange exchange = publicApiExchange();
        WebFilterChain chain = e -> Mono.empty();
        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void returns429WhenLimitExceeded() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(31L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = publicApiExchange();
        WebFilterChain chain = e -> Mono.empty();
        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    private static MockServerWebExchange publicApiExchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/players")
                        .header(ApiKeyAuthHeaders.X_ORG_ID, "org-1")
                        .header(ApiKeyAuthHeaders.X_PLAN_TIER, "FREE")
                        .build());
    }
}
