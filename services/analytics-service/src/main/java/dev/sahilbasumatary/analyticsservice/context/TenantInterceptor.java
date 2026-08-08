package dev.sahilbasumatary.analyticsservice.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ORG_ID = "X-Org-Id";

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            log.warn(
                    "Missing {} header for {} {}",
                    HEADER_USER_ID,
                    request.getMethod(),
                    request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        RequestContext.setUserId(userId.trim());
        String orgId = request.getHeader(HEADER_ORG_ID);
        if (orgId != null && !orgId.isBlank()) {
            RequestContext.setOrgId(orgId.trim());
        }
        log.debug(
                "Tenant context set - userId: {}, orgId: {}",
                RequestContext.getUserId(),
                RequestContext.getOrgId());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        RequestContext.clear();
    }
}
