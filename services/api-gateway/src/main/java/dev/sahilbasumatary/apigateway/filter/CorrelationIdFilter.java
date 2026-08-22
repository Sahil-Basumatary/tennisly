package dev.sahilbasumatary.apigateway.filter;

import dev.sahilbasumatary.common.observability.RequestIds;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming =
                RequestIds.resolve(
                        exchange.getRequest().getHeaders().getFirst(RequestIds.REQUEST_ID_HEADER),
                        exchange.getRequest().getHeaders().getFirst(RequestIds.TRACEPARENT_HEADER));
        ServerHttpRequest mutated =
                exchange
                        .getRequest()
                        .mutate()
                        .header(RequestIds.REQUEST_ID_HEADER, incoming)
                        .build();
        exchange.getResponse().getHeaders().set(RequestIds.REQUEST_ID_HEADER, incoming);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
