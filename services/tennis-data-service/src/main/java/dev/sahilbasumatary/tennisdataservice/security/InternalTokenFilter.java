package dev.sahilbasumatary.tennisdataservice.security;

import dev.sahilbasumatary.common.security.InternalToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Free-tier Render services are publicly routable; this is the lock that keeps
 * only the gateway (and sibling services that share the secret) in.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class InternalTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public InternalTokenFilter(@Value("${tennisly.internal-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!InternalToken.isEnabled(expectedToken)) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || "/actuator/health".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(InternalToken.HEADER);
        if (!InternalToken.matches(expectedToken, provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"missing_or_invalid_gateway_token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
