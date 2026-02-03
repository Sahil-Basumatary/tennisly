package dev.sahilbasumatary.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {
    private static final String FALLBACK_IP = "unknown";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            return Mono.just(request != null ? resolveClientIp(request) : FALLBACK_IP);
        };
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            return FALLBACK_IP;
        }
        InetAddress address = remoteAddress.getAddress();
        if (address == null) {
            return FALLBACK_IP;
        }
        return address.getHostAddress();
    }
}
