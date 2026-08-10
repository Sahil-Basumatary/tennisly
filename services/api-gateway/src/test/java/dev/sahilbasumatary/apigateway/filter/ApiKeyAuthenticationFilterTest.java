package dev.sahilbasumatary.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class ApiKeyAuthenticationFilterTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID KEY_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    private MockWebServer userService;

    @BeforeEach
    void startServer() throws IOException {
        userService = new MockWebServer();
        userService.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        if (userService != null) {
            userService.shutdown();
        }
    }

    private ApiKeyAuthenticationFilter filter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ExchangeStrategies strategies =
                ExchangeStrategies.builder()
                        .codecs(
                                configurer -> {
                                    configurer
                                            .defaultCodecs()
                                            .jackson2JsonEncoder(
                                                    new Jackson2JsonEncoder(
                                                            mapper, MediaType.APPLICATION_JSON));
                                    configurer
                                            .defaultCodecs()
                                            .jackson2JsonDecoder(
                                                    new Jackson2JsonDecoder(
                                                            mapper, MediaType.APPLICATION_JSON));
                                })
                        .build();
        WebClient client =
                WebClient.builder()
                        .baseUrl("http://127.0.0.1:" + userService.getPort())
                        .exchangeStrategies(strategies)
                        .build();
        return new ApiKeyAuthenticationFilter(client);
    }

    @Test
    void skipsNonPublicApiPaths() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/matches").build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        WebFilterChain chain =
                e -> {
                    seen.set(e);
                    return Mono.empty();
                };
        filter().filter(exchange, chain).block();
        assertEquals(exchange, seen.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void missingApiKeyReturnsUnauthorized() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/players").build());
        filter().filter(exchange, e -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void blankApiKeyReturnsUnauthorized() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/players")
                                .header(ApiKeyAuthHeaders.X_API_KEY, "  ")
                                .build());
        filter().filter(exchange, e -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void validApiKeyAppliesTrustedHeadersAndContinues() {
        String payload =
                "{\"organizationId\":\""
                        + ORG_ID
                        + "\",\"apiKeyId\":\""
                        + KEY_ID
                        + "\",\"scopes\":[\"read\",\"players\"],\"planTier\":\"PRO\",\"organizationName\":\"Baseline Club\"}";
        userService.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .setBody(payload));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/players")
                                .header(ApiKeyAuthHeaders.X_API_KEY, "tly_live_test")
                                .header(JwtClaimsForwardingFilter.X_USER_ID, "spoofed")
                                .build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        filter()
                .filter(
                        exchange,
                        e -> {
                            seen.set(e);
                            return Mono.empty();
                        })
                .block();
        assertNull(exchange.getResponse().getStatusCode());
        assertEquals(
                "apikey:" + KEY_ID,
                seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
        assertEquals(
                ORG_ID.toString(),
                seen.get().getRequest().getHeaders().getFirst(ApiKeyAuthHeaders.X_ORG_ID));
        assertEquals(
                "PRO",
                seen.get().getRequest().getHeaders().getFirst(ApiKeyAuthHeaders.X_PLAN_TIER));
    }

    @Test
    void invalidApiKeyFromUpstreamReturnsUnauthorized() {
        userService.enqueue(new MockResponse().setResponseCode(401));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/players")
                                .header(ApiKeyAuthHeaders.X_API_KEY, "bad")
                                .build());
        filter().filter(exchange, e -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void upstreamErrorReturnsUnauthorized() throws IOException {
        userService.shutdown();
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/players")
                                .header(ApiKeyAuthHeaders.X_API_KEY, "tly_live_test")
                                .build());
        filter().filter(exchange, e -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
