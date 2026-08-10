package dev.sahilbasumatary.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class JwtClaimsForwardingFilterTest {

    private final JwtClaimsForwardingFilter filter = new JwtClaimsForwardingFilter();

    @Test
    void stripsSpoofedHeadersWhenUnauthenticated() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/matches")
                                .header(JwtClaimsForwardingFilter.X_USER_ID, "attacker")
                                .header(JwtClaimsForwardingFilter.X_ORG_ID, "attacker-org")
                                .header(JwtClaimsForwardingFilter.X_USER_ROLES, "ADMIN")
                                .build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        WebFilterChain chain =
                e -> {
                    seen.set(e);
                    return Mono.empty();
                };
        filter.filter(exchange, chain).block();
        assertNull(seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
        assertNull(seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_ORG_ID));
        assertNull(
                seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ROLES));
    }

    @Test
    void forwardsSubjectOrgAndRolesListFromJwt() {
        Jwt jwt =
                jwt(
                        Map.of(
                                "org_id",
                                "org-42",
                                "roles",
                                List.of("ADMIN", "MEMBER")));
        assertForwarded(jwt, "user-1", "org-42", "ADMIN,MEMBER");
    }

    @Test
    void fallsBackToOrgRoleThenRoleClaim() {
        Jwt orgRoleOnly = jwt(Map.of("org_id", "org-7", "org_role", "OWNER"));
        assertForwarded(orgRoleOnly, "user-1", "org-7", "OWNER");
        Jwt roleOnly = jwt(Map.of("role", "PLAYER"));
        assertForwarded(roleOnly, "user-1", null, "PLAYER");
    }

    @Test
    void ignoresNonJwtAuthentication() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/matches")
                                .header(JwtClaimsForwardingFilter.X_USER_ID, "spoof")
                                .build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        filter.filter(
                        exchange,
                        e -> {
                            seen.set(e);
                            return Mono.empty();
                        })
                .contextWrite(
                        ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken("local", "n/a")))
                .block();
        assertNull(seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
    }

    private void assertForwarded(Jwt jwt, String userId, String orgId, String roles) {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/matches")
                                .header(JwtClaimsForwardingFilter.X_USER_ID, "spoof")
                                .build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        filter.filter(
                        exchange,
                        e -> {
                            seen.set(e);
                            return Mono.empty();
                        })
                .contextWrite(
                        ReactiveSecurityContextHolder.withAuthentication(
                                new JwtAuthenticationToken(jwt)))
                .block();
        assertEquals(
                userId,
                seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ID));
        if (orgId == null) {
            assertNull(
                    seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_ORG_ID));
        } else {
            assertEquals(
                    orgId,
                    seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_ORG_ID));
        }
        assertEquals(
                roles,
                seen.get().getRequest().getHeaders().getFirst(JwtClaimsForwardingFilter.X_USER_ROLES));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                .claims(c -> c.putAll(claims))
                .build();
    }
}
