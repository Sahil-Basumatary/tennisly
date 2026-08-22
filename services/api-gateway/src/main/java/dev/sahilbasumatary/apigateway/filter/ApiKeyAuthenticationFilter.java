package dev.sahilbasumatary.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.apigateway.client.ApiKeyValidationRequest;
import dev.sahilbasumatary.apigateway.client.ApiKeyValidationResponse;
import dev.sahilbasumatary.apigateway.metrics.GatewayTimers;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthenticationFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String PUBLIC_API_PREFIX = "/api/v1/";
    private static final String CACHE_PREFIX = "apikey:valid:";

    private final WebClient userServiceWebClient;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;
    private final GatewayTimers timers;

    public ApiKeyAuthenticationFilter(
            WebClient userServiceWebClient,
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${gateway.api-key.cache-ttl-seconds:30}") long cacheTtlSeconds) {
        this(userServiceWebClient, redisTemplate, objectMapper, cacheTtlSeconds, null);
    }

    @Autowired
    public ApiKeyAuthenticationFilter(
            WebClient userServiceWebClient,
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${gateway.api-key.cache-ttl-seconds:30}") long cacheTtlSeconds,
            GatewayTimers timers) {
        this.userServiceWebClient = userServiceWebClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = Duration.ofSeconds(Math.max(1, cacheTtlSeconds));
        this.timers = timers;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(PUBLIC_API_PREFIX)) {
            return chain.filter(exchange);
        }
        String apiKey = exchange.getRequest().getHeaders().getFirst(ApiKeyAuthHeaders.X_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            return unauthorized(exchange, "missing_api_key");
        }
        return validateApiKey(apiKey.trim())
                .flatMap(
                        validation -> {
                            var mutatedRequest =
                                    ApiKeyAuthHeaders.applyTrustedHeaders(
                                            exchange.getRequest(), validation);
                            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                                    .thenReturn(Boolean.TRUE);
                        })
                .switchIfEmpty(
                        Mono.defer(
                                () ->
                                        unauthorized(exchange, "invalid_api_key")
                                                .thenReturn(Boolean.FALSE)))
                .then();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    private Mono<ApiKeyValidationResponse> validateApiKey(String apiKey) {
        String cacheKey = CACHE_PREFIX + sha256Hex(apiKey);
        Timer.Sample sample = timers == null ? null : Timer.start();
        return redisTemplate
                .opsForValue()
                .get(cacheKey)
                .flatMap(this::deserializeCached)
                .doOnSuccess(v -> stopTimer(sample, true))
                .switchIfEmpty(
                        Mono.defer(
                                () ->
                                        validateAndCache(apiKey, cacheKey)
                                                .doOnSuccess(v -> stopTimer(sample, false))));
    }

    private void stopTimer(Timer.Sample sample, boolean hit) {
        if (sample == null || timers == null) {
            return;
        }
        sample.stop(hit ? timers.apiKeyHit() : timers.apiKeyMiss());
    }

    private Mono<ApiKeyValidationResponse> validateAndCache(String apiKey, String cacheKey) {
        return userServiceWebClient
                .post()
                .uri("/internal/api-keys/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ApiKeyValidationRequest(apiKey))
                .retrieve()
                .bodyToMono(ApiKeyValidationResponse.class)
                .flatMap(
                        validation ->
                                serializeCached(validation)
                                        .flatMap(
                                                json ->
                                                        redisTemplate
                                                                .opsForValue()
                                                                .set(cacheKey, json, cacheTtl)
                                                                .onErrorResume(
                                                                        ex -> {
                                                                            log.debug(
                                                                                    "api-key cache"
                                                                                        + " write"
                                                                                        + " failed:"
                                                                                        + " {}",
                                                                                    ex
                                                                                            .getMessage());
                                                                            return Mono
                                                                                    .just(false);
                                                                        })
                                                                .thenReturn(validation))
                                        .defaultIfEmpty(validation))
                .onErrorResume(ex -> Mono.empty());
    }

    private Mono<ApiKeyValidationResponse> deserializeCached(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, ApiKeyValidationResponse.class));
        } catch (JsonProcessingException ex) {
            log.debug("api-key cache decode failed: {}", ex.getMessage());
            return Mono.empty();
        }
    }

    private Mono<String> serializeCached(ApiKeyValidationResponse validation) {
        try {
            return Mono.just(objectMapper.writeValueAsString(validation));
        } catch (JsonProcessingException ex) {
            return Mono.empty();
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
