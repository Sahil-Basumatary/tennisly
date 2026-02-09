package dev.sahilbasumatary.apigateway.filter;

import java.util.stream.StreamSupport;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class JwtClaimsForwardingFilter implements WebFilter, Ordered {

    public static final String X_USER_ID = "X-User-Id";
    public static final String X_ORG_ID = "X-Org-Id";
    public static final String X_USER_ROLES = "X-User-Roles";

    private static final String CLAIM_ORG_ID = "org_id";
    private static final String CLAIM_ORG_ROLE = "org_role";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ROLES = "roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Strip internal headers first to prevent external clients from spoofing identity
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(this::stripInternalHeaders)
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitized).build();
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(this::isJwtAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    ServerHttpRequest mutated = sanitizedExchange.getRequest().mutate()
                            .headers(headers -> addClaimHeaders(headers, jwt))
                            .build();
                    return chain.filter(sanitizedExchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(sanitizedExchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private boolean isJwtAuthentication(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof Jwt;
    }

    private void stripInternalHeaders(HttpHeaders headers) {
        headers.remove(X_USER_ID);
        headers.remove(X_ORG_ID);
        headers.remove(X_USER_ROLES);
    }

    private void addClaimHeaders(HttpHeaders headers, Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub != null && !sub.isBlank()) {
            headers.set(X_USER_ID, sub);
        }
        String orgId = getClaimAsString(jwt, CLAIM_ORG_ID);
        if (orgId != null && !orgId.isBlank()) {
            headers.set(X_ORG_ID, orgId);
        }
        String roles = resolveRoles(jwt);
        if (roles != null && !roles.isBlank()) {
            headers.set(X_USER_ROLES, roles);
        }
    }

    private String getClaimAsString(Jwt jwt, String claim) {
        Object value = jwt.getClaim(claim);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private String resolveRoles(Jwt jwt) {
        Object roles = jwt.getClaim(CLAIM_ROLES);
        if (roles instanceof Iterable<?> iterable) {
            String joined =
                    StreamSupport.stream(iterable.spliterator(), false)
                            .map(Object::toString)
                            .reduce((a, b) -> a + "," + b)
                            .orElse(null);
            if (joined != null && !joined.isBlank()) {
                return joined;
            }
        }
        if (roles != null) {
            return roles.toString();
        }
        String orgRole = getClaimAsString(jwt, CLAIM_ORG_ROLE);
        if (orgRole != null && !orgRole.isBlank()) {
            return orgRole;
        }
        String role = getClaimAsString(jwt, CLAIM_ROLE);
        if (role != null && !role.isBlank()) {
            return role;
        }
        return null;
    }
}
