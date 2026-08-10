package dev.sahilbasumatary.apigateway.filter;

import dev.sahilbasumatary.common.security.InternalToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Stamps every proxied request so free-tier backends can reject anything that
 * did not come through this gateway (or another holder of the same secret).
 */
@Component
public class InternalTokenRelayFilter implements GlobalFilter, Ordered {

    private final String token;

    public InternalTokenRelayFilter(@Value("${tennisly.internal-token:}") String token) {
        this.token = token;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!InternalToken.isEnabled(token)) {
            return chain.filter(exchange);
        }
        ServerWebExchange mutated =
                exchange.mutate()
                        .request(builder -> builder.header(InternalToken.HEADER, token))
                        .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
