package dev.sahilbasumatary.notificationservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.common.security.InternalToken;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalTokenFilterTest {

    @Test
    void skipsWhenTokenDisabled() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("");
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notifications/admin/deliveries");
        request.setRequestURI("/api/notifications/admin/deliveries");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void allowsHealthWithoutToken() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRequestURI("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void rejectsMissingTokenWhenEnabled() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notifications/admin/deliveries");
        request.setRequestURI("/api/notifications/admin/deliveries");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("missing_or_invalid_gateway_token"));
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notifications/admin/deliveries");
        request.setRequestURI("/api/notifications/admin/deliveries");
        request.addHeader(InternalToken.HEADER, "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }
}
