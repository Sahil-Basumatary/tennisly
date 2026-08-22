package dev.sahilbasumatary.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.sahilbasumatary.common.observability.RequestIds;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    @Test
    void stampsMissingRequestId() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/matches").build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        new CorrelationIdFilter()
                .filter(
                        exchange,
                        e -> {
                            seen.set(e);
                            return Mono.empty();
                        })
                .block();
        String stamped = exchange.getResponse().getHeaders().getFirst(RequestIds.REQUEST_ID_HEADER);
        assertNotNull(stamped);
        assertEquals(
                stamped, seen.get().getRequest().getHeaders().getFirst(RequestIds.REQUEST_ID_HEADER));
    }

    @Test
    void keepsIncomingRequestId() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/matches")
                                .header(RequestIds.REQUEST_ID_HEADER, "req-1")
                                .build());
        new CorrelationIdFilter().filter(exchange, e -> Mono.empty()).block();
        assertEquals(
                "req-1", exchange.getResponse().getHeaders().getFirst(RequestIds.REQUEST_ID_HEADER));
    }
}
