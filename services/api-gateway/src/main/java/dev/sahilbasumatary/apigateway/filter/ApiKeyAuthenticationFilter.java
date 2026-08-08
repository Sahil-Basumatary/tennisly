package dev.sahilbasumatary.apigateway.filter;

import dev.sahilbasumatary.apigateway.client.ApiKeyValidationRequest;
import dev.sahilbasumatary.apigateway.client.ApiKeyValidationResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
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

    private static final String PUBLIC_API_PREFIX = "/api/v1/";

    private final WebClient userServiceWebClient;

    public ApiKeyAuthenticationFilter(WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
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
                            return chain.filter(
                                    exchange.mutate().request(mutatedRequest).build());
                        })
                .switchIfEmpty(unauthorized(exchange, "invalid_api_key"));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private Mono<ApiKeyValidationResponse> validateApiKey(String apiKey) {
        return userServiceWebClient
                .post()
                .uri("/internal/api-keys/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ApiKeyValidationRequest(apiKey))
                .exchangeToMono(
                        response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return response.bodyToMono(ApiKeyValidationResponse.class);
                            }
                            return Mono.empty();
                        })
                .onErrorResume(ex -> Mono.empty());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
